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

import org.apache.commons.codec.binary.Hex;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

public class CommandStrategy implements IStrategy {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<Optional<BotApiMethod>> runStrategy(Update update) {

        ArrayList<Optional<BotApiMethod>> result = new ArrayList<>();
        try {
            if (update.hasMessage()) {
                String messageText = update.getMessage().getText();

                if (messageText.contains("!forgive")) {
                        int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                        DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                        ConcurrentMap map = database.hashMap("user").createOrOpen();
                        ConcurrentMap warn = database.hashMap("warnings").createOrOpen();
                        map.remove(userId);
                        warn.remove(userId);
                        database.close();
                        SendMessage msg = new SendMessage();
                        msg.setChatId(update.getMessage().getChatId());
                        msg.setText("All is forgiven, " + update.getMessage().getReplyToMessage().getFrom().getFirstName() + ", Chomp loves you!");
                        result.add(Optional.of(msg));
                        log.info("User " + update.getMessage().getReplyToMessage().getFrom().getFirstName() + " was forgiven");
                        return result;

                } else if (messageText.contains("!warn")) {

                        int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                        DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                        ConcurrentMap map = database.hashMap("warnings").createOrOpen();

                        int count = 1;
                        if (!map.containsKey(userId)) {
                            map.put(userId, count);
                        } else {
                            int balance = (int) map.get(userId);
                            balance += 1;
                            map.put(userId, balance);
                            if (balance >= 3) {
                                map.remove(userId);
                                result.add(banUser(userId, update.getMessage().getChatId()));
                            }
                            count = balance;
                        }
                        database.close();

                        SendMessage msg = new SendMessage();
                        msg.setChatId(update.getMessage().getChatId());
                        String firstName = update.getMessage().getReplyToMessage().getFrom().getFirstName();

                        if(count == 1) {
                            msg.setText(firstName + ", please rethink what you are doing.\nKindly requested 1/3 times.");
                            log.info("User " + firstName + " was warned");
                        } else if(count == 2) {
                            msg.setText(firstName + ", please rethink what you are doing or this will not end well.\nKindly requested 2/3 times.");
                            log.info("User " + firstName + " was warned");
                        } else {
                            msg.setText(firstName + " was warned 3/3 times - game over, Chomp wins!");
                            log.info("User " + firstName + " was banned");
                        }

                        result.add(Optional.of(msg));
                        return result;

                } else if (messageText.contains("!mute")) {

                        int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                        long timeToMute = Instant.now().getEpochSecond();
                        if (messageText.contains("!mute1")) timeToMute += 3600;
                        else if (messageText.contains("!mute24")) timeToMute += 86400;
                        else if (messageText.contains("!mute48")) timeToMute += 172800;
                        RestrictChatMember mute = new RestrictChatMember();
                        mute.setUserId(userId);
                        mute.setChatId(update.getMessage().getChatId());
                        mute.setUntilDate(new BigDecimal(timeToMute).intValueExact());
                        mute.setCanAddWebPagePreviews(false);
                        mute.setCanSendMessages(false);
                        mute.setCanSendMediaMessages(false);
                        mute.setCanSendOtherMessages(false);
                        log.info("User " + update.getMessage().getReplyToMessage().getFrom().getFirstName() + " was muted");
                        result.add(Optional.of(mute));
                        return result;

                } else if (messageText.contains("!unmute")) {

                        int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                        RestrictChatMember unmute = new RestrictChatMember();
                        unmute.setUserId(userId);
                        unmute.setChatId(update.getMessage().getChatId());
                        unmute.setCanAddWebPagePreviews(true);
                        unmute.setCanSendMessages(true);
                        unmute.setCanSendMediaMessages(true);
                        unmute.setCanSendOtherMessages(true);
                        log.info("User " + update.getMessage().getReplyToMessage().getFrom().getFirstName() + " was unmuted");
                        result.add(Optional.of(unmute));
                        return result;

                } else if (messageText.contains("!unban")) {
                        int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                        UnbanChatMember unban = new UnbanChatMember();
                        unban.setUserId(userId);
                        unban.setChatId(update.getMessage().getChatId());
                        log.info("User " + update.getMessage().getReplyToMessage().getFrom().getFirstName() + " was unbanned");
                        result.add(Optional.of(unban));
                        return result;

                } else if (messageText.contains("!trust")) {

                        int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                        log.info("User " + update.getMessage().getReplyToMessage().getFrom().getFirstName() + " was added to whitelist");
                        DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                        ConcurrentMap userWhitelist = database.hashMap("trustedUser").createOrOpen();
                        userWhitelist.put(userId, userId);
                        database.close();

                } else if (messageText.contains("!delete")) {

                        int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                        if (update.getMessage().getReplyToMessage().hasPhoto()) {
                            List<PhotoSize> photos = update.getMessage().getReplyToMessage().getPhoto();
                            DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                            ConcurrentMap imageBlackList = database.hashMap("imageBlacklist").createOrOpen();

                            String photoMeta = "";
                            for (PhotoSize photo : photos) {
                                photoMeta += photo.getHeight().toString() + photo.getWidth().toString();
                            }

                            byte[] bytesOfPhoto = photoMeta.getBytes();
                            try {
                                MessageDigest md = MessageDigest.getInstance("SHA-256");
                                imageBlackList.put(new String(Hex.encodeHex(md.digest(bytesOfPhoto))), userId);
                                database.close();
                            } catch (NoSuchAlgorithmException e) {
                                database.close();
                                log.error(e.getMessage());
                            }
                            database.close();
                        }
                        result.add(Optional.of(new DeleteMessage(update.getMessage().getChatId(), update.getMessage().getReplyToMessage().getMessageId())));
                        result.add(Optional.of(new DeleteMessage(update.getMessage().getChatId(), update.getMessage().getMessageId())));
                        return result;
                }
            }

            result.add(Optional.empty());
            return result;

        } catch (NullPointerException e) {
            result.add(Optional.empty());
            return result;
        }
    }

    private Optional<BotApiMethod> banUser(int userId, long chatId){
        KickChatMember ban = new KickChatMember();
        ban.setUserId(userId);
        ban.setChatId(chatId);
        ban.setUntilDate(new BigDecimal(Instant.now().getEpochSecond()+864000).intValueExact());
        return Optional.of(ban);
    }
}
