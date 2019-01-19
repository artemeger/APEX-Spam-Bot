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
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

public class DeleteLinksStrategy implements IStrategy {

    @Override
    @SuppressWarnings("unchecked")
    public Optional<BotApiMethod> runStrategy(Update update) {
        int userId = update.getMessage().getFrom().getId();
        String website = update.getMessage().getConnectedWebsite();
        User fromUser = update.getMessage().getForwardFrom();
        Chat fromChat = update.getMessage().getForwardFromChat();
        List<MessageEntity> msgList = update.getMessage().getEntities();
        boolean hasLink = false;
        if(msgList != null){
            for (MessageEntity ent : msgList){
                if(ent.getType().contains("link")) hasLink = true;
            }
        }

        if(website != null || fromUser != null || fromChat != null || hasLink){
            DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
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
                    return Optional.of(new DeleteMessage(update.getMessage().getChatId(), update.getMessage().getMessageId()));
                }
            }
        }
        return Optional.empty();
    }
}
