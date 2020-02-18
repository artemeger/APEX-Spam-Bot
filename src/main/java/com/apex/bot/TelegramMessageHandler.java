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

import com.apex.objects.Feedback;
import com.apex.strategy.CommandStrategy;
import com.apex.strategy.DeleteFileStrategy;
import com.apex.strategy.DeleteLinksStrategy;
import com.apex.strategy.IStrategy;
import org.json.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class TelegramMessageHandler extends ATelegramBot {

    private IStrategy deleteFile = new DeleteFileStrategy();
    private IStrategy deleteLinks = new DeleteLinksStrategy();
    private IStrategy runCommand = new CommandStrategy();
    private List<Integer> WHITELIST;
    private List<Long> CHAT;
    public static long VERIFICATON;

    TelegramMessageHandler(String token, String botname) throws IOException {
        super(token, botname);
        setup();
    }

    @SuppressWarnings("unchecked")
    private void setup() throws IOException {
        final JSONObject config = new JSONObject(new String(Files.readAllBytes(Paths.get("token.json"))));
        WHITELIST = (List<Integer>) config.toMap().get("whitelist");
        VERIFICATON = (long) config.toMap().get("verification");
        CHAT = (List<Long>) config.toMap().get("chat");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onUpdateReceived(Update update) {

        try {

            if(update.hasCallbackQuery()){
                try {
                    CallbackQuery query = update.getCallbackQuery();
                    String callbackData = query.getData();
                    if(callbackData != null && !callbackData.equals("false")){

                        String [] arg = callbackData.split(",");

                        DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                        ConcurrentMap map = database.hashMap("feedback").createOrOpen();
                        Feedback feedback = (Feedback) map.get(arg[1]);
                        database.close();

                        if(arg[0].equals("blacklist")) {
                            database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                            ConcurrentMap mapUrlBlackList = database.hashMap("urlBlackList").createOrOpen();
                            mapUrlBlackList.put(feedback.getDataToBan(), feedback.getUserId());
                            database.close();
                        }

                        try {
                            KickChatMember ban = new KickChatMember();
                            ban.setUserId(feedback.getUserId());
                            ban.setChatId(feedback.getChatId());
                            ban.setUntilDate(new BigDecimal(Instant.now().getEpochSecond()).intValue());
                            execute(ban);
                        } catch (Exception e){
                            log.info("Cant ban - User already deleted");
                        }
                    }

                    DeleteMessage deleteMessage = new DeleteMessage(VERIFICATON,  query.getMessage().getMessageId());
                    execute(deleteMessage);
                } catch (Exception e){
                    log.error("Error in Callback");
                    log.error(e.getMessage());
                }
            }

            if (CHAT.contains(update.getMessage().getChatId())) {
                if (update.getMessage().getNewChatMembers() != null) {
                    DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                    ConcurrentMap userMap = database.hashMap("user").createOrOpen();
                    for (User user : update.getMessage().getNewChatMembers()) {
                        userMap.put(user.getId(), Instant.now().getEpochSecond());
                    }
                    database.close();
                    log.info("Added User");
                }

                int from = update.getMessage().getFrom().getId();
                ArrayList<Optional<BotApiMethod>> commands;

                if(update.hasMessage()) {
                    if (WHITELIST.contains(from)) {
                        commands = runCommand.runStrategy(update);
                        for (Optional<BotApiMethod> method : commands) {
                            method.ifPresent(command -> {
                                try {
                                    execute(command);
                                    log.info("Command fired");
                                } catch (TelegramApiException e) {
                                    log.error("Failed execute Command" + e.getMessage());
                                }
                            });
                        }
                    }
                }

                if (!WHITELIST.contains(from)) {
                    if (update.getMessage().hasDocument()) {
                        commands = deleteFile.runStrategy(update);
                        for (Optional<BotApiMethod> method : commands) {
                            method.ifPresent(delete -> {
                                try {
                                    execute(delete);
                                    log.info("Deleted File");
                                } catch (TelegramApiException e) {
                                    log.error("Failed to delete File" + e.getMessage());
                                }
                            });
                        }
                    }

                    commands = deleteLinks.runStrategy(update);
                    for (Optional<BotApiMethod> method : commands) {
                        method.ifPresent(delete -> {
                            try {
                                execute(delete);
                                log.info("Deleted Link");
                            } catch (TelegramApiException e) {
                                log.error("Failed to delete Link" + e.getMessage());
                            }
                        });
                    }
                }
            }

        } catch (Exception e) {}
    }
}
