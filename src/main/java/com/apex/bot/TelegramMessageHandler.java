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

import com.apex.strategy.CommandStrategy;
import com.apex.strategy.DeleteFileStrategy;
import com.apex.strategy.DeleteLinksStrategy;
import com.apex.strategy.IStrategy;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class TelegramMessageHandler extends ATelegramBot {

    private IStrategy deleteFile = new DeleteFileStrategy();
    private IStrategy deleteLinks = new DeleteLinksStrategy();
    private IStrategy runCommand = new CommandStrategy();
    private static final List<Integer> WHITELIST = Arrays.asList(512328408, 521684737, 533756221, 331773699, 516271269, 497516201, 454184647);
    private static final List<Long> CHAT = Arrays.asList(-1001385910531L, -1001175224299L);

    TelegramMessageHandler(String token, String botname) {
        super(token, botname);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onUpdateReceived(Update update) {
        try {
            if (CHAT.contains(update.getMessage().getChatId())) {
                try {
                    if (update.getMessage().getNewChatMembers() != null) {
                        DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                        ConcurrentMap userMap = database.hashMap("user").createOrOpen();
                        for (User user : update.getMessage().getNewChatMembers()) {
                            userMap.put(user.getId(), Instant.now().getEpochSecond());
                        }
                        database.close();
                        log.info("Added User");
                    }

                    if (update.hasMessage()) {
                        int from = update.getMessage().getFrom().getId();

                        if (WHITELIST.contains(from)) {
                            runCommand.runStrategy(update).ifPresent(command -> {
                                try {
                                    execute(command);
                                    log.info("Command fired");
                                } catch (TelegramApiException e) {
                                    log.error("Failed execute Command" + e.getMessage());
                                }
                            });
                        }

                        deleteLinks.runStrategy(update).ifPresent(delete -> {
                            try {
                                execute(delete);
                                log.info("Deleted Link");
                            } catch (TelegramApiException e) {
                                log.error("Failed to delete Link" + e.getMessage());
                            }
                        });
                    }

                    if (update.getMessage().hasDocument()) {
                        deleteFile.runStrategy(update).ifPresent(delete -> {
                            try {
                                execute(delete);
                                log.info("Deleted File");
                            } catch (TelegramApiException e) {
                                log.error("Failed to delete File" + e.getMessage());
                            }
                        });
                    }

                } catch (Exception e) {
                    log.debug("Exception caught " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("Exception caught " + e.getMessage());
        }
    }
}
