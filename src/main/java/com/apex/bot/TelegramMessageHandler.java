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

import crypto.CPXKey;
import message.request.cmd.GetAccountCmd;
import message.request.cmd.SendRawTransactionCmd;
import message.response.ExecResult;
import message.transaction.FixedNumber;
import message.transaction.Transaction;
import message.transaction.TransactionType;
import message.util.GenericJacksonWriter;
import message.util.RequestCallerService;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigInteger;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class TelegramMessageHandler extends ATelegramBot {

    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private RequestCallerService caller;

    private GenericJacksonWriter writer;

    private static final String START = "Welcome to the APEX Network blockchain distribution bot\n\n" +
            "The purpose of this bot is to allow community members and others wanting to explore the different functionalities of the blockchain to eaily request a batch of CPX test coins to be delivered to their APEX public address\n\n" +
            "- Before you request your CPX, you must have an APEX public address. This can easily be created on our web wallet integrated in the blockchain explorer at tracker.apexnetwork.io\n\n" +
            "- The limit per address is 1000 CPX per week, 1000 CPX will be distributed to your APEX public address immediately, and if needed more can be requested 7 days later.\n\n" +
            "- The CPX can be used for sending transactions, voting, refunding votes and playing games on the APEX Network blockchain\n\n" +
            "To proceed, simply copy and paste your APEX Network public address to the bot, funds will be visible in your address on tracker.apexnetwork.io shortly after!\n\n" +
            "Our APEX Network blackjack game can be found at http://blackjack.apexnetwork.io";

    TelegramMessageHandler(String token, String botname){
        super(token, botname);
        caller = new RequestCallerService();
        writer = new GenericJacksonWriter();
    }

    @Override
    public void onUpdateReceived(Update update) {
        executor.submit(() -> {
            final long fromChat = update.getMessage().getChatId();
            final int userId = update.getMessage().getFrom().getId();
            try {
                if (update.hasMessage()) {
                    SendMessage response = new SendMessage();
                    response.setChatId(fromChat);
                    final String msg = update.getMessage().getText();
                    if (msg.contains("/start")) {
                        response.setText(START);
                        execute(response);
                    } else if(msg.startsWith("AP") && msg.length() == 35){
                        try {
                            final String scriptHash = CPXKey.getScriptHashFromCPXAddress(msg);
                            log.info(msg);
                            TGUser currentUser = SpamBot.getRepo().find(ObjectFilters.eq("telegramId", userId)).firstOrDefault();
                            log.info("Current user");
                            if(currentUser != null){
                                if(currentUser.getNextRequest() <= Instant.now().toEpochMilli()){
                                    executeTransaction(SpamBot.getPrivateKey(), scriptHash);
                                    currentUser.setPaid(currentUser.getPaid() + 1000);
                                    currentUser.setNextRequest(Instant.now().toEpochMilli() + 604800000L);
                                    SpamBot.getRepo().update(currentUser);
                                    response.setText("Success! I have transferred 1000 CPX to: " + msg);
                                    execute(response);
                                } else {
                                    response.setText("You have reached your request limit.\n\nNext transfer will be available after " +
                                            new Date(currentUser.getNextRequest()).toString());
                                    execute(response);
                                }
                            } else {
                                TGUser newUser = new TGUser(update.getMessage().getFrom().getUserName(), msg, userId, Instant.now().toEpochMilli() + 604800000L, 1000);
                                SpamBot.getRepo().insert(newUser);
                                executeTransaction(SpamBot.getPrivateKey(), scriptHash);
                                response.setText("Success! I have transferred 1000 CPX to: " + msg);
                                execute(response);
                            }
                        } catch (Exception e){
                            log.error(e.getMessage());
                            response.setText("Passed address was not a valid CPX mainnet address!\n\nMake sure you dont use a NEP-5 NEO address");
                            execute(response);
                        }
                    } else {
                        response.setText("Please pass the public address you want to receive the funds on");
                        execute(response);
                    }
                }
            } catch (Exception e) {}
        });
    }

    private void executeTransaction(ECPrivateKey privateKey, String scriptHash) {
        try {
            final Transaction tx = Transaction.builder()
                    .txType(TransactionType.TRANSFER)
                    .fromPubKeyHash(CPXKey.getScriptHash(privateKey))
                    .toPubKeyHash(scriptHash)
                    .amount(new FixedNumber(1000.0))
                    .nonce(getAccountNonce(CPXKey.getPublicAddressCPX(privateKey)))
                    .data(new byte[0])
                    .gasPrice(new FixedNumber(0.0000000001))
                    .gasLimit(BigInteger.valueOf(300000L))
                    .version(1)
                    .executeTime(Instant.now().toEpochMilli())
                    .build();
            final SendRawTransactionCmd cmd = new SendRawTransactionCmd(tx.getBytes(SpamBot.getCrypto(), privateKey));
            caller.postRequest(SpamBot.getRpcUrl(), cmd);
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    private HashMap<String, Object> getAccount(String address){
        try{
            final GetAccountCmd cmd = new GetAccountCmd(address);
            final String response = caller.postRequest(SpamBot.getRpcUrl(), cmd);
            return writer.getObjectFromString(ExecResult.class, response).getResult();
        } catch (Exception e){
            log.error("In getAccountBalance(): " + e.getMessage());
            return new HashMap<>();
        }
    }

    private long getAccountNonce(String address) {
        HashMap<String, Object> accountMap = getAccount(address);
        return (long) (int) accountMap.get("nextNonce");
    }

}
