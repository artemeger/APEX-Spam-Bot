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
import crypto.CryptoService;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.generics.LongPollingBot;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.interfaces.ECPrivateKey;

public class SpamBot {

    private static final String BOT_NAME = "DistributionBot";
    private static final Logger LOG = LoggerFactory.getLogger(SpamBot.class);
    private static String rpcUrl;
    private static ECPrivateKey privateKey;
    private final static CryptoService crypto = new CryptoService();
    private static ObjectRepository<TGUser> repository;

    public SpamBot(){
        repository = Nitrite.builder().filePath("database.db")
                .openOrCreate().getRepository(TGUser.class);
    }

    public static void main(String[] args){
        TelegramSessionManager telegramSessionManager = new TelegramSessionManager();
        try {
            String content = new String(Files.readAllBytes(Paths.get("token.json")));
            JSONObject questions = new JSONObject(content);
            final String apiToken = (String) questions.toMap().get("token");
            rpcUrl = (String) questions.toMap().get("rpcUrl");
            privateKey = crypto.getECPrivateKeyFromRawString(CPXKey.getRawFromWIF((String) questions.toMap().get("privateKey")));
            LongPollingBot bot = new TelegramMessageHandler(apiToken, BOT_NAME);
            telegramSessionManager.addPollingBot(bot);
            telegramSessionManager.start();
            LOG.info("Bot started");
            while (true){
                Thread.sleep(500);
            }
        } catch (Exception e) {
            LOG.error("Something went wrong: "+ e.getCause().getMessage());
        } finally {
            telegramSessionManager.stop();
            LOG.info("Bot down");
        }
    }

    public static String getRpcUrl() {
        return rpcUrl;
    }

    public static ECPrivateKey getPrivateKey() {
        return privateKey;
    }

    public static CryptoService getCrypto() {
        return crypto;
    }

    public static ObjectRepository<TGUser> getRepo() {
        return repository;
    }

}
