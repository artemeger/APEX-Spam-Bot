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

import com.apex.addition.FeedbackKeyboard;
import com.apex.entities.Blacklist;
import com.apex.entities.TGUser;
import com.apex.repository.IBlackListRepository;
import com.apex.repository.IFeedbackRepository;
import com.apex.repository.ITGUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

@Component
public class DeleteStrategy implements IStrategy {

    @Autowired
    private IFeedbackRepository feedbackRepository;

    @Autowired
    private ITGUserRepository tgUserRepository;

    @Autowired
    private IBlackListRepository blackListRepository;

    @Value("${bot.verification}")
    private long verification;

    @Value("${bot.mimetypes}")
    private List<String> mimeTypes;

    @Value("${bot.filenames}")
    private List<String> fileNames;

    @Override
    public ArrayList<BotApiMethod> runStrategy(Update update) {

        final ArrayList<BotApiMethod> result = new ArrayList<>();
        final int userId = update.getMessage().getFrom().getId();
        final long chatId = update.getMessage().getChatId();
        final int messageId = update.getMessage().getMessageId();
        final List<MessageEntity> msgList = update.getMessage().getEntities();

        final Optional<TGUser> userOpt = tgUserRepository.findById(userId);
        if(userOpt.isPresent()){
            if(userOpt.get().isTrusted()) return result;
        }

        if (update.getMessage().hasDocument()) {
            final Document doc = update.getMessage().getDocument();
            final String mimeType = doc.getMimeType();
            final String fileName = doc.getFileName();
            if (mimeTypes.contains(mimeType) &&
                    fileNames.contains(fileName.substring(fileName.indexOf(".") + 1).trim())) {
                return result;
            } else {
                final String hash = doc.getFileId() + doc.getFileName() +
                        doc.getMimeType() + doc.getFileSize().toString();
                return checkHashForBlacklist(hash, userId, chatId, messageId);
            }
        }

        if(update.getMessage().hasPhoto()) {
            final String hash = update.getMessage().getPhoto().stream()
                    .map(photo -> photo.getWidth().toString() +
                                photo.getHeight().toString() +
                                photo.getFileSize().toString())
                    .collect(Collectors.joining());
            return checkHashForBlacklist(hash, userId, chatId, messageId);
        }

        if(msgList != null){
            for (MessageEntity ent : msgList){
                if(ent.getType().contains("link") || ent.getType().contains("url")) {
                    String link = "";
                    if(ent.getText() != null) link = ent.getText();
                    if(ent.getUrl() != null) link = ent.getUrl();
                    if(!link.equals("")) return checkHashForBlacklist(link, userId, chatId, messageId);
                }
            }
        }

        return result;
    }

    private ArrayList<BotApiMethod> checkHashForBlacklist(final String data, final int userId,
                                                          final long chatId, final int messageId){
        final ArrayList<BotApiMethod> result = new ArrayList<>();
        final CRC32 crc32 = new CRC32();
        crc32.update(data.getBytes());
        final String hash = String.valueOf(crc32.getValue());
        final Optional<Blacklist> blacklistOpt = blackListRepository.findFirstByHash(hash);
        if(blacklistOpt.isEmpty()){
            result.add(new ForwardMessage(verification, chatId, messageId));
            result.add(new FeedbackKeyboard(userId, chatId, verification, hash, feedbackRepository).getBanKeyboard());
        } else {
            result.add(getBan(userId, chatId));
        }
        result.add(new DeleteMessage(chatId, messageId));
        return result;
    }

    private KickChatMember getBan(final int userId, final long chatId){
        KickChatMember ban = new KickChatMember();
        ban.setUserId(userId);
        ban.setChatId(chatId);
        ban.setUntilDate(new BigDecimal(Instant.now().getEpochSecond()).intValue());
        return ban;
    }

}
