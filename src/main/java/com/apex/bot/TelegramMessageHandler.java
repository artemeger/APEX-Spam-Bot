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

import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TelegramMessageHandler extends ATelegramBot {

    private static final List<Integer> WHITELIST = Arrays.asList(512328408, 521684737, 533756221, 331773699, 516271269, 497516201, 454184647, 32845648);
    private static final List<Long> CHAT = Arrays.asList(-1001472315014L);
    private static final String REGEX = "[^a-zA-Z0-9]";
    private static final String INTRO = "Lets begin! \n";
    private static final String OUTRO = "The Quiz has finished. Thank you for participating!";
    private AtomicBoolean quizStartedLock = new AtomicBoolean(false);
    private AtomicBoolean currentQuestionIsAnswered = new AtomicBoolean(true);
    private Map<String, Object> qMap;
    private HashMap<Integer, Integer> resultMap = new HashMap<>();
    private HashMap<Integer, String> userNameMap = new HashMap<>();
    private String currentAnswer = "";
    private int iterator = 1;


    TelegramMessageHandler(String token, String botname) throws IOException {
        super(token, botname);
        String content = new String(Files.readAllBytes(Paths.get("questions.json")));
        JSONObject questions = new JSONObject(content);
        qMap = questions.toMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onUpdateReceived(Update update) {
        try {
            if (CHAT.contains(update.getMessage().getChatId())) {

                int from = update.getMessage().getFrom().getId();

                if(update.hasMessage()) {
                    String msg = update.getMessage().getText();
                    long chatId = update.getMessage().getChatId();
                    User user = update.getMessage().getFrom();
                    if (WHITELIST.contains(from)) {
                        if (msg.contains("!start")) {
                            if(!quizStartedLock.get()) {
                                quizStartedLock.set(true);
                                SendMessage sendMessage = new SendMessage();
                                sendMessage.setChatId(chatId);
                                sendMessage.setText(INTRO);
                                execute(sendMessage);
                                execute(nextQuestion(chatId, qMap.keySet().iterator().next()));
                            }
                        } else if (msg.contains("!next")) {
                            if(currentQuestionIsAnswered.get() && qMap.size() > 0)
                            execute(nextQuestion(chatId, qMap.keySet().iterator().next()));
                        } else if (msg.contains("!result")) {
                            SendMessage resultMessage = new SendMessage();
                            resultMessage.setChatId(chatId);
                            String standings;
                            if(qMap.size() > 0)
                                standings = "CURRENT ONGOING STANDINGS:\n";
                            else
                                standings = "FINAL STANDINGS:\n";
                            Map<Integer, Integer> sortedMap = resultMap
                                    .entrySet()
                                    .stream()
                                    .sorted(Map.Entry.comparingByValue())
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                            LinkedHashMap::new));
                            int counter = 1;
                            for(int userId : sortedMap.keySet()){
                                standings += counter + ". " + userNameMap.get(userId) + " --> " + resultMap.get(userId);
                            }
                            resultMessage.setText(standings);
                            execute(resultMessage);
                        }
                    }
                    String cleanedUpMsg = msg.toLowerCase().replaceAll(REGEX, "");
                    if(cleanedUpMsg.equals(currentAnswer.toLowerCase().replaceAll(REGEX, "")) && quizStartedLock.get()){
                        if(!currentQuestionIsAnswered.get()) {
                            if (resultMap.containsKey(user.getId())) {
                                resultMap.put(user.getId(), resultMap.get(user.getId()) + 1);
                            }
                            else{
                                resultMap.put(user.getId(), 1);
                                if(user.getLastName() != null)
                                userNameMap.put(user.getId(), user.getFirstName() + " " + user.getLastName());
                                else userNameMap.put(user.getId(), user.getFirstName());
                            }
                            SendMessage sendSuccess = new SendMessage();
                            sendSuccess.setChatId(chatId);
                            sendSuccess.setText("Well done " + user.getFirstName() + "!\n" +
                                    "\"" + currentAnswer + "\" was the right answer. You get the point for that.");
                            execute(sendSuccess);
                            currentQuestionIsAnswered.set(true);
                        }
                        if(qMap.size() == 0){
                            SendMessage sendFinished = new SendMessage();
                            sendFinished.setChatId(chatId);
                            sendFinished.setText(OUTRO);
                            execute(sendFinished);
                        }
                    }
                }
            }
        } catch (Exception e) {}
    }

    private SendMessage nextQuestion(long chatId, String question){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Question "+ iterator + ":\n" + question);
        currentAnswer = (String) qMap.get(question);
        currentQuestionIsAnswered.set(false);
        qMap.remove(question);
        iterator ++;
        return sendMessage;
    }

}
