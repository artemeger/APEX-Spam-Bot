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

package com.apex;

import com.apex.bot.TelegramMessageHandler;
import com.apex.bot.TelegramSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class SpamBot {

    @Autowired
    private TelegramSessionManager telegramSessionManager;

    @Autowired
    private TelegramMessageHandler telegramMessageHandler;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args){
        SpringApplication.run(SpamBot.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runTelegramBot(){
        try {
            telegramSessionManager.addPollingBot(telegramMessageHandler);
            telegramSessionManager.start();
            log.info("Bot started");
        } catch (Exception e) {
            log.error("Something went wrong: "+ e.getMessage());
        } finally {
            telegramSessionManager.stop();
            log.info("Bot down");
        }
    }

}
