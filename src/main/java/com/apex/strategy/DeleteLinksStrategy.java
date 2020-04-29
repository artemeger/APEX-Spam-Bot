/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 - 2019
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.apex.strategy;

import com.apex.bot.TelegramMessageHandler;
import com.apex.objects.Feedback;
import org.apache.commons.codec.binary.Hex;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class DeleteLinksStrategy implements IStrategy {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<Optional<BotApiMethod>> runStrategy(Update update) {
        ArrayList<Optional<BotApiMethod>> result = new ArrayList<>();
        result.add(Optional.empty());

        int userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String website = update.getMessage().getConnectedWebsite();
        User fromUser = update.getMessage().getForwardFrom();
        Chat fromChat = update.getMessage().getForwardFromChat();
        List<MessageEntity> msgList = update.getMessage().getEntities();

        DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
        ConcurrentMap userWhitelist = database.hashMap("trustedUser").createOrOpen();
        boolean isWhitelisted = userWhitelist.containsKey(userId);
        database.close();
        if(isWhitelisted) return result;

        if (update.getMessage().hasDocument()) {
            if (update.getMessage().getDocument().getMimeType().equals("image/gif") ||
                update.getMessage().getDocument().getMimeType().equals("video/mp4")) {
                return result;
            }
        }

        if(update.getMessage().hasPhoto()) {
            List<PhotoSize> photos = update.getMessage().getPhoto();
            database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
            ConcurrentMap imageBlackList = database.hashMap("imageBlacklist").createOrOpen();

            String photoMeta = "";
            for(PhotoSize photo : photos){
                photoMeta += photo.getHeight().toString() + photo.getWidth().toString();
            }

            byte[] bytesOfPhoto = photoMeta.getBytes();
            String photoId = "";

            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                photoId = new String(Hex.encodeHex(md.digest(bytesOfPhoto)));
            } catch (NoSuchAlgorithmException e) {
                log.error(e.getMessage());
            }
            boolean isBlacklistedImage = imageBlackList.containsKey(photoId);
            database.close();

            if (!isBlacklistedImage){
                result.add(Optional.of(new ForwardMessage(TelegramMessageHandler.VERIFICATON, chatId, update.getMessage().getMessageId())));
                result.add(sendBanKeyboard(userId, chatId));
            }
            result.add(Optional.of(new DeleteMessage(chatId, update.getMessage().getMessageId())));
            return result;
        }

        boolean hasLink = false;
        String link = "";

        if(msgList != null){
            for (MessageEntity ent : msgList){
                if(ent.getType().contains("link") || ent.getType().contains("url")) {
                    hasLink = true;
                    if(ent.getText() != null) link = ent.getText();
                    if(ent.getUrl() != null) link = ent.getUrl();
                    database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                    ConcurrentMap mapUrlBlackList = database.hashMap("urlBlackList").createOrOpen();
                    boolean isBlacklistedUrl = mapUrlBlackList.containsKey(link);
                    database.close();
                    if(isBlacklistedUrl){
                        KickChatMember ban = new KickChatMember();
                        ban.setUserId(userId);
                        ban.setChatId(chatId);
                        ban.setUntilDate(new BigDecimal(Instant.now().getEpochSecond()).intValue());
                        result.add(Optional.of(ban));
                        result.add(Optional.of(new DeleteMessage(chatId, update.getMessage().getMessageId())));
                        return result;
                    }
                }
            }
        }

        if(hasLink || update.getMessage().hasPhoto()){
            if(!link.equals("")) {
                result.add(Optional.of(new ForwardMessage(TelegramMessageHandler.VERIFICATON, update.getMessage().getChatId(), update.getMessage().getMessageId())));
                result.add(sendFeedbackKeyboard(link, userId, chatId));
            }
            result.add(Optional.of(new DeleteMessage(chatId, update.getMessage().getMessageId())));
            return result;
        }

        if(website != null || fromUser != null || fromChat != null || update.getMessage().hasDocument()) {
            database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
            ConcurrentMap map = database.hashMap("user").createOrOpen();
            boolean isInDb = map.containsKey(userId);
            long timeCreated = 0;
            if(isInDb) timeCreated = (long) map.get(userId);
            database.close();
            if(isInDb){
                if(Instant.now().getEpochSecond() - timeCreated < 864000){
                    database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                    map = database.hashMap("user").createOrOpen();
                    map.put(userId, Instant.now().getEpochSecond());
                    database.close();

                    if(update.getMessage().hasDocument() || update.getMessage().hasPhoto()){
                        if((update.getMessage().getCaption()!= null && update.getMessage().getCaption().contains("@")) ||
                                (update.getMessage().getText() != null && update.getMessage().getText().contains("@"))) {
                            result.add(Optional.of(new ForwardMessage(TelegramMessageHandler.VERIFICATON, chatId, update.getMessage().getMessageId())));
                            result.add(sendBanKeyboard(userId, chatId));
                        }
                    }

                    result.add(Optional.of(new DeleteMessage(update.getMessage().getChatId(), update.getMessage().getMessageId())));
                    return result;
                }
            }
        }
        return result;
    }

    private Optional<BotApiMethod> sendFeedbackKeyboard(String dataToBan, int userId, long chatid) {
        SendMessage message = new SendMessage();
        message.setChatId(TelegramMessageHandler.VERIFICATON);
        message.setText("Choose which action to take");
        DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
        ConcurrentMap map = database.hashMap("feedback").createOrOpen();
        String id = UUID.randomUUID().toString();
        map.put(id, new Feedback(dataToBan, userId, chatid));
        database.close();
        try {
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText("Blacklist content and ban");
            button1.setCallbackData("blacklist,"+id);
            row1.add(button1);
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton button2 = new InlineKeyboardButton();
            button2.setText("Whitelist user");
            button2.setCallbackData("whitelist,"+id);
            row2.add(button2);
            List<InlineKeyboardButton> row3 = new ArrayList<>();
            InlineKeyboardButton button3 = new InlineKeyboardButton();
            button3.setText("No action");
            button3.setCallbackData("false");
            row3.add(button2);
            keyboard.add(row1);
            keyboard.add(row2);
            keyboard.add(row3);
            inlineKeyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(inlineKeyboardMarkup);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return Optional.of(message);
    }

    private Optional<BotApiMethod> sendBanKeyboard(int userId, long chatid) {
        SendMessage message = new SendMessage();
        message.setChatId(TelegramMessageHandler.VERIFICATON);
        message.setText("Ban this user?");
        DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
        ConcurrentMap map = database.hashMap("feedback").createOrOpen();
        String id = UUID.randomUUID().toString();
        map.put(id, new Feedback("", userId, chatid));
        database.close();
        try {
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText("Yes");
            button1.setCallbackData("ban,"+id);
            row1.add(button1);
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton button2 = new InlineKeyboardButton();
            button2.setText("No");
            button2.setCallbackData("false");
            row2.add(button2);
            keyboard.add(row1);
            keyboard.add(row2);
            inlineKeyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(inlineKeyboardMarkup);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return Optional.of(message);
    }
}
