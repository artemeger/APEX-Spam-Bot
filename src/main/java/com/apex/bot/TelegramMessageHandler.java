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
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class TelegramMessageHandler extends ATelegramBot {

    private String REGEX = "[^a-zA-Z0-9 ]";
    private List<Integer> whitelist;
    private long chat;
    private String intro;
    private String outro;
    private AtomicBoolean quizStartedLock;
    private AtomicBoolean currentQuestionIsAnswered;
    private ArrayList<Map<String, Object>> questionsList;
    private HashMap<Integer, Integer> resultMap;
    private HashMap<Integer, String> userNameMap;
    private ArrayList<String> keywords;
    private int iterator;
    private int threshold;


    TelegramMessageHandler(String token, String botname) throws IOException {
        super(token, botname);
        setup();
    }

    private void setup() throws IOException {
        iterator = 1;
        threshold = 0;
        quizStartedLock = new AtomicBoolean(false);
        currentQuestionIsAnswered = new AtomicBoolean(true);
        questionsList = new ArrayList<>();
        resultMap = new HashMap<>();
        userNameMap = new HashMap<>();
        keywords = new ArrayList<>();

        final JSONObject questions = new JSONObject(new String(Files.readAllBytes(Paths.get("questions.json"))));
        questions.toMap().values().forEach(q -> questionsList.add((Map<String, Object>)q));
        final JSONObject config = new JSONObject(new String(Files.readAllBytes(Paths.get("token.json"))));
        whitelist = (List<Integer>) config.toMap().get("whitelist");
        chat = (long) config.toMap().get("chat");
        intro = (String) config.toMap().get("intro");
        outro = (String) config.toMap().get("outro");
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.getMessage().getText() != null && chat == update.getMessage().getChatId()) {
                int from = update.getMessage().getFrom().getId();
                if(update.hasMessage()) {
                    String msg = update.getMessage().getText();
                    long chatId = update.getMessage().getChatId();
                    User user = update.getMessage().getFrom();
                    if (whitelist.contains(from)) {
                        if (msg.contains("!start") && !quizStartedLock.get()) {
                            quizStartedLock.set(true);
                            execute(startQuiz(chatId));
                            execute(nextQuestion(chatId, questionsList.iterator().next()));
                        } else if (msg.contains("!next") && quizStartedLock.get() &&
                                currentQuestionIsAnswered.get() && !questionsList.isEmpty()) {
                            execute(nextQuestion(chatId, questionsList.iterator().next()));
                        } else if (msg.contains("!reset") && quizStartedLock.get()) {
                            setup();
                            quizStartedLock.set(true);
                            execute(startQuiz(chatId));
                            execute(nextQuestion(chatId, questionsList.iterator().next()));
                        } else if (msg.contains("!result") && quizStartedLock.get()) {
                            SendMessage resultMessage = new SendMessage();
                            resultMessage.setChatId(chatId);
                            String standings = questionsList.iterator().hasNext() ? "CURRENT ONGOING STANDINGS\n" : "FINAL STANDINGS\n";
                            Map<Integer, Integer> sortedMap = resultMap
                                    .entrySet()
                                    .stream()
                                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                            LinkedHashMap::new));
                            int counter = 1;
                            for(int userId : sortedMap.keySet()){
                                standings += counter + ". " + userNameMap.get(userId) + "   <(( "+resultMap.get(userId)+" ))>\n";
                                counter ++;
                            }
                            resultMessage.setText(standings);
                            execute(resultMessage);
                        }
                    }

                    String cleanedUpMsg = msg.toLowerCase().replaceAll(REGEX, "");

                    ArrayList<String> passedAnswer =  new ArrayList<>(Arrays.asList(cleanedUpMsg.split(" ")));
                    int countKeyWords = 0;
                    for(String word : keywords){
                        if(passedAnswer.contains(word))
                            countKeyWords++;
                    }

                    if(countKeyWords >= threshold && quizStartedLock.get()){
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
                                    "\"" + msg + "\" was the right answer. You get the point for that.");
                            execute(sendSuccess);
                            currentQuestionIsAnswered.set(true);
                        }
                        if(questionsList.isEmpty()){
                            SendMessage sendFinished = new SendMessage();
                            sendFinished.setChatId(chatId);
                            sendFinished.setText(outro);
                            execute(sendFinished);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            Stream.of(e.getStackTrace()).forEach(stackTraceElement -> log.error(stackTraceElement.toString()));
        }
    }

    private SendMessage nextQuestion(long chatId, Map<String, Object> question){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Question "+ iterator + ":\n" + question.get("question"));
        threshold = (int) question.get("threshold");
        String currentAnswer = (String) question.get("keywords");
        keywords = new ArrayList<>(Arrays.asList(currentAnswer.toLowerCase().replaceAll(REGEX, "").split(" ")));
        currentQuestionIsAnswered.set(false);
        questionsList.remove(question);
        iterator ++;
        return sendMessage;
    }

    private SendMessage startQuiz(long chatId){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(intro);
        return sendMessage;
    }

}
