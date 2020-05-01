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

package com.apex.bot;

import com.apex.ATelegramBot;
import com.apex.addition.FeedbackAction;
import com.apex.entities.Blacklist;
import com.apex.entities.Feedback;
import com.apex.entities.TGUser;
import com.apex.repository.IBlackListRepository;
import com.apex.repository.IFeedbackRepository;
import com.apex.repository.ITGUserRepository;
import com.apex.strategy.CommandStrategy;
import com.apex.strategy.DeleteStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class TelegramMessageHandler extends ATelegramBot {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${bot.verification}")
    private long verification;

    @Value("${bot.whitelist}")
    private List<Integer> whitelist;

    @Value("${bot.chat}")
    private List<Long> chat;

    @Autowired
    private IFeedbackRepository feedbackRepository;

    @Autowired
    private ITGUserRepository tgUserRepository;

    @Autowired
    private IBlackListRepository blackListRepository;

    @Autowired
    private DeleteStrategy deleteLinks;

    @Autowired
    private CommandStrategy runCommand;

    @Autowired
    public TelegramMessageHandler(@Value("${bot.token}") String botToken, @Value("${bot.name}") String botName) {
        super(botToken, botName);
    }

    @Override
    public void onUpdateReceived(Update update) {

        try {

            if (update.hasCallbackQuery()) {
                try {
                    final CallbackQuery query = update.getCallbackQuery();
                    final String callbackData = query.getData();
                    if (callbackData != null) {
                        final String[] arg = callbackData.split(",");
                        final String action = arg[0];
                        final long feedbackId = Long.parseLong(arg[1]);
                        final Optional<Feedback> feedbackOpt = feedbackRepository.findById(feedbackId);
                        feedbackOpt.ifPresent(feedback -> {
                            if (action.equals(FeedbackAction.BAN.getAction())) {
                                if (!feedback.getData().equals(""))
                                    blackListRepository.save(new Blacklist(feedback.getData()));
                                try {
                                    KickChatMember ban = new KickChatMember();
                                    ban.setUserId(feedback.getUserId());
                                    ban.setChatId(feedback.getChatId());
                                    ban.setUntilDate(new BigDecimal(Instant.now().getEpochSecond()).intValue());
                                    execute(ban);
                                } catch (Exception e) {
                                    log.info("Cant ban User with id " + feedback.getUserId());
                                }
                            } else if (action.equals(FeedbackAction.WHITELIST.getAction())) {
                                tgUserRepository.save(new TGUser(feedback.getUserId(), 0, true));
                                log.info("Whitelist user with id " + feedback.getUserId());
                            } else if (callbackData.equals(FeedbackAction.IGNORE.getAction())){
                                log.info("Ignore feedback");
                            }
                            feedbackRepository.delete(feedback);
                        });
                    }
                    execute(new DeleteMessage(verification, query.getMessage().getMessageId()));
                } catch (Exception e) {
                    log.error("Error in Callback");
                    log.error(e.getMessage());
                }
            }

            if (update.getMessage() != null) {

                final long chatId = update.getMessage().getChatId();
                final int fromUser = update.getMessage().getFrom().getId();

                if (chat.contains(chatId)) {

                    final ArrayList<BotApiMethod> commands = new ArrayList<>();

                    if (whitelist.contains(fromUser)) {
                        if (update.hasMessage()) {
                            commands.addAll(runCommand.runStrategy(update));
                        }
                    } else {
                        commands.addAll(deleteLinks.runStrategy(update));
                    }

                    commands.forEach(command -> {
                        try {
                            execute(command);
                        } catch (TelegramApiException e) {}
                    });

                }
            }
        } catch (Exception e) {
            log.info("Got an unknown message. Ignore");
        }
    }
}
