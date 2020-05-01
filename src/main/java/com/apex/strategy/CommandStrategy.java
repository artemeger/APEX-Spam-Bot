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

import com.apex.entities.TGUser;
import com.apex.repository.IBlackListRepository;
import com.apex.repository.ITGUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

@Component
public class CommandStrategy implements IStrategy {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ITGUserRepository tgUserRepository;

    @Autowired
    private IBlackListRepository blackListRepository;

    @Value("${first.warning}")
    private String firstWarning;

    @Value("${second.warning}")
    private String secondWarning;

    @Value("${third.warning}")
    private String thirdWarning;

    @Override
    public ArrayList<BotApiMethod> runStrategy(Update update) {

        final ArrayList<BotApiMethod> result = new ArrayList<>();
        try {
            if (update.hasMessage()) {

                final String messageText = update.getMessage().getText();
                final int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                final long chatId = update.getMessage().getChatId();
                final String userName = update.getMessage().getReplyToMessage().getFrom().getFirstName();

                if (messageText.contains("!forgive")) {
                        tgUserRepository.findById(userId).ifPresent(user -> {
                            tgUserRepository.delete(user);
                            final SendMessage msg = new SendMessage();
                            msg.setChatId(update.getMessage().getChatId());
                            msg.setText("All is forgiven, " + userName + ", I love you! (again)");
                            result.add(msg);
                            log.info("User " + userName + " was forgiven");
                        });
                } else if (messageText.contains("!warn")) {
                    final SendMessage msg = new SendMessage();
                    msg.setChatId(chatId);
                    tgUserRepository.findById(userId).ifPresentOrElse(user -> {
                        user.setCount(user.getCount() + 1);
                        if (user.getCount() == 1){
                            msg.setText(userName + ", " + firstWarning);
                        } else if (user.getCount() == 2){
                            msg.setText(userName + ", " + secondWarning);
                        } else if(user.getCount() >= 3) {
                            result.add(banUser(userId, chatId));
                            msg.setText(userName + " " + thirdWarning);
                        }
                        tgUserRepository.save(user);
                    }, () -> {
                        tgUserRepository.save(new TGUser(userId, 1, true));
                        msg.setText(userName + firstWarning);
                    });
                    result.add(msg);
                } else if (messageText.contains("!mute")) {
                        long timeToMute = Instant.now().getEpochSecond();
                        if (messageText.contains("!mute1")) timeToMute += 3600;
                        else if (messageText.contains("!mute24")) timeToMute += 86400;
                        else if (messageText.contains("!mute48")) timeToMute += 172800;
                        final RestrictChatMember mute = new RestrictChatMember();
                        mute.setUserId(userId);
                        mute.setChatId(chatId);
                        mute.setUntilDate(new BigDecimal(timeToMute).intValueExact());
                        mute.setCanAddWebPagePreviews(false);
                        mute.setCanSendMessages(false);
                        mute.setCanSendMediaMessages(false);
                        mute.setCanSendOtherMessages(false);
                        log.info("User " + userName + " was muted");
                        result.add(mute);
                } else if (messageText.contains("!unmute")) {
                        RestrictChatMember unmute = new RestrictChatMember();
                        unmute.setUserId(userId);
                        unmute.setChatId(chatId);
                        unmute.setCanAddWebPagePreviews(true);
                        unmute.setCanSendMessages(true);
                        unmute.setCanSendMediaMessages(true);
                        unmute.setCanSendOtherMessages(true);
                        log.info("User " + userName + " was unmuted");
                        result.add(unmute);
                } else if (messageText.contains("!unban")) {
                        UnbanChatMember unban = new UnbanChatMember();
                        unban.setUserId(userId);
                        unban.setChatId(chatId);
                        log.info("User " + userName + " was unbanned");
                        result.add(unban);
                } else if (messageText.contains("!trust")) {
                        tgUserRepository.findById(userId).ifPresentOrElse(user -> {
                            user.setTrusted(true);
                            tgUserRepository.save(user);
                        }, () -> tgUserRepository.save(new TGUser(userId, 0, true)));
                        log.info("User " + userName + " was trusted");
                }
            }
        } catch (NullPointerException e) {}
        return result;
    }

    private BotApiMethod banUser(int userId, long chatId){
        KickChatMember ban = new KickChatMember();
        ban.setUserId(userId);
        ban.setChatId(chatId);
        ban.setUntilDate(new BigDecimal(Instant.now().getEpochSecond() + 864000).intValueExact());
        return ban;
    }

}
