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

@Component
public class DeleteLinksStrategy implements IStrategy {

    @Autowired
    private IFeedbackRepository feedbackRepository;

    @Autowired
    private ITGUserRepository tgUserRepository;

    @Autowired
    private IBlackListRepository blackListRepository;

    @Value("${bot.verification}")
    private long verification;

    @Override
    public ArrayList<BotApiMethod> runStrategy(Update update) {

        final ArrayList<BotApiMethod> result = new ArrayList<>();
        final int userId = update.getMessage().getFrom().getId();
        final long chatId = update.getMessage().getChatId();
        final int messageId = update.getMessage().getMessageId();
        final String website = update.getMessage().getConnectedWebsite();
        final User fromUser = update.getMessage().getForwardFrom();
        final Chat fromChat = update.getMessage().getForwardFromChat();
        final List<MessageEntity> msgList = update.getMessage().getEntities();

        final Optional<TGUser> userOpt = tgUserRepository.findById(userId);
        if(userOpt.isPresent()){
            if(userOpt.get().isTrusted()) return result;
        }

        if (update.getMessage().hasDocument()) {
            if (update.getMessage().getDocument().getMimeType().equals("image/gif") ||
                update.getMessage().getDocument().getMimeType().equals("video/mp4")) {
                return result;
            }
        }

        if(update.getMessage().hasPhoto()) {
            final String hash = update.getMessage().getReplyToMessage().getPhoto().stream()
                    .map(photo -> photo.getHeight().toString()
                            + photo.getWidth().toString()
                            + photo.getFileId())
                    .collect(Collectors.joining());
            final Optional<Blacklist> blacklistOpt = blackListRepository.findFirstByHash(hash);
            if(!blacklistOpt.isPresent()){
                result.add(new ForwardMessage(verification, chatId, messageId));
                result.add(new FeedbackKeyboard(userId, chatId, verification, hash, feedbackRepository).getBanKeyboard());
            }
            result.add(new DeleteMessage(chatId, update.getMessage().getMessageId()));
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
                    final Optional<Blacklist> blacklistOpt = blackListRepository.findFirstByHash(link);
                    if(blacklistOpt.isPresent()){
                        KickChatMember ban = new KickChatMember();
                        ban.setUserId(userId);
                        ban.setChatId(chatId);
                        ban.setUntilDate(new BigDecimal(Instant.now().getEpochSecond()).intValue());
                        result.add(ban);
                        result.add(new DeleteMessage(chatId, messageId));
                        return result;
                    }
                }
            }
        }

        if(hasLink || update.getMessage().hasPhoto()){
            if(!link.equals("")) {
                result.add(new ForwardMessage(verification, chatId, messageId));
                result.add(new FeedbackKeyboard(userId, chatId, verification, link, feedbackRepository).getBanKeyboard());
            }
            result.add(new DeleteMessage(chatId, update.getMessage().getMessageId()));
            return result;
        }

        if(website != null || fromUser != null || fromChat != null || update.getMessage().hasDocument()) {
            if(update.getMessage().hasDocument() || update.getMessage().hasPhoto()){
                if((update.getMessage().getCaption()!= null && update.getMessage().getCaption().contains("@")) ||
                        (update.getMessage().getText() != null && update.getMessage().getText().contains("@"))) {
                    result.add(new ForwardMessage(verification, chatId, messageId));
                    result.add(new FeedbackKeyboard(userId, chatId,verification, "", feedbackRepository).getBanKeyboard());
                }
            }
            result.add(new DeleteMessage(chatId, messageId));
        }

        return result;

    }

}
