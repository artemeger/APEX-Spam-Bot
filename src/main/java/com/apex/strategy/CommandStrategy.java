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

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

public class CommandStrategy implements IStrategy {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    @SuppressWarnings("unchecked")
    public Optional<BotApiMethod> runStrategy(Update update) {
        if(update.hasMessage()) {
            String messageText = update.getMessage().getText();

            if(messageText.contains("!forgive")){
                try {
                    int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                    DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                    ConcurrentMap map = database.hashMap("user").createOrOpen();
                    ConcurrentMap warn = database.hashMap("warnings").createOrOpen();
                    map.remove(userId);
                    warn.remove(userId);
                    database.close();
                    log.info("User "+ update.getMessage().getReplyToMessage().getFrom().getFirstName() +" was forgiven");
                } catch (NullPointerException e){
                    return Optional.empty();
                }
            }

            else if(messageText.contains("!warn")){
                try {
                    int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                    DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                    ConcurrentMap map = database.hashMap("warnings").createOrOpen();
                    int count = 1;
                    if(!map.containsKey(userId)){
                        map.put(userId, count);
                    } else {
                        int balance = (int) map.get(userId);
                        balance += 1;
                        if(balance >= 3){
                            map.remove(userId);
                            database.close();
                            log.info("User "+ update.getMessage().getReplyToMessage().getFrom().getFirstName() +" was banned");
                            return banUser(userId, update.getMessage().getChatId());
                        }
                        count = balance;
                        map.put(userId, balance);
                    }
                    database.close();
                    SendMessage msg = new SendMessage();
                    msg.setChatId(update.getMessage().getChatId());
                    msg.setText(update.getMessage().getReplyToMessage().getFrom().getFirstName()+ " you are warned! \n"+
                            "Count is: " + String.valueOf(count)+ "/3");
                    log.info("User "+ update.getMessage().getReplyToMessage().getFrom().getFirstName() +" was warned");
                    return Optional.of(msg);
                } catch (NullPointerException e){
                    return Optional.empty();
                }
            }

            else if(messageText.contains("!mute")){
                try {
                    int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                    RestrictChatMember mute = new RestrictChatMember();
                    mute.setUserId(userId);
                    mute.setChatId(update.getMessage().getChatId());
                    mute.setUntilDate(new BigDecimal(Instant.now().getEpochSecond()+864000).intValueExact());
                    mute.setCanAddWebPagePreviews(false);
                    mute.setCanSendMessages(false);
                    mute.setCanSendMediaMessages(false);
                    mute.setCanSendOtherMessages(false);
                    log.info("User "+ update.getMessage().getReplyToMessage().getFrom().getFirstName() +" was muted");
                    return Optional.of(mute);
                } catch (NullPointerException e){
                    return Optional.empty();
                }
            }

            else if(messageText.contains("!unmute")){
                try {
                    int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                    RestrictChatMember unmute = new RestrictChatMember();
                    unmute.setUserId(userId);
                    unmute.setChatId(update.getMessage().getChatId());
                    unmute.setCanAddWebPagePreviews(true);
                    unmute.setCanSendMessages(true);
                    unmute.setCanSendMediaMessages(true);
                    unmute.setCanSendOtherMessages(true);
                    log.info("User "+ update.getMessage().getReplyToMessage().getFrom().getFirstName() +" was unmuted");
                    return Optional.of(unmute);
                } catch (NullPointerException e){
                    return Optional.empty();
                }
            }

            else if(messageText.contains("!unban")){
                try {
                    int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                    UnbanChatMember unban = new UnbanChatMember();
                    unban.setUserId(userId);
                    unban.setChatId(update.getMessage().getChatId());
                    log.info("User "+ update.getMessage().getReplyToMessage().getFrom().getFirstName() +" was unbanned");
                    return Optional.of(unban);
                } catch (NullPointerException e){
                    return Optional.empty();
                }
            }

        }
        return Optional.empty();
    }

    private Optional<BotApiMethod> banUser(int userId, long chatId){
        KickChatMember ban = new KickChatMember();
        ban.setUserId(userId);
        ban.setChatId(chatId);
        ban.setUntilDate(new BigDecimal(Instant.now().getEpochSecond()+864000).intValueExact());
        return Optional.of(ban);
    }
}
