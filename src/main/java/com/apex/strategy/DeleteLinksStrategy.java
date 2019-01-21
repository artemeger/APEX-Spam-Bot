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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

public class DeleteLinksStrategy implements IStrategy {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<Optional<BotApiMethod>> runStrategy(Update update) {
        ArrayList<Optional<BotApiMethod>> result = new ArrayList<>();
        result.add(Optional.empty());

        int userId = update.getMessage().getFrom().getId();
        String website = update.getMessage().getConnectedWebsite();
        User fromUser = update.getMessage().getForwardFrom();
        Chat fromChat = update.getMessage().getForwardFromChat();
        List<MessageEntity> msgList = update.getMessage().getEntities();

        DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
        ConcurrentMap userWhitelist = database.hashMap("trustedUser").createOrOpen();
        boolean isWhitelisted = userWhitelist.containsKey(userId);
        database.close();
        if(isWhitelisted) return result;

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

            if (imageBlackList.containsKey(photoId)){
                result.add(Optional.of(new DeleteMessage(update.getMessage().getChatId(), update.getMessage().getMessageId())));
                database.close();
                return result;
            }

            database.close();
        }

        boolean hasLink = false;
        if(msgList != null){
            for (MessageEntity ent : msgList){
                if(ent.getType().contains("link") || ent.getType().contains("url")) hasLink = true;
            }
        }

        if((update.getMessage().getFrom().getUserName() == null && hasLink) ||
           (update.getMessage().getFrom().getUserName() == null && update.getMessage().hasPhoto())){
            result.add(Optional.of(new DeleteMessage(update.getMessage().getChatId(), update.getMessage().getMessageId())));
            return result;
        }

        if(website != null || fromUser != null || fromChat != null || hasLink) {
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
                    result.add(Optional.of(new DeleteMessage(update.getMessage().getChatId(), update.getMessage().getMessageId())));
                    return result;
                }
            }
        }
        result.add(Optional.empty());
        return result;
    }
}
