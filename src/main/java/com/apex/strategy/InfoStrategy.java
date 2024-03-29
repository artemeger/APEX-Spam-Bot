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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.util.ArrayList;

import static java.lang.String.format;

@Component
public class InfoStrategy implements IStrategy {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${promo}")
    private String promo;

    @Value("${nextcommand}")
    private String nextCommand;

    @Override
    public ArrayList<BotApiMethod> runStrategy(Update update) {

        final ArrayList<BotApiMethod> result = new ArrayList<>();
        try {
            if (update.hasMessage()) {
                final String messageText = update.getMessage().getText();
                final String userName = update.getMessage().getReplyToMessage().getFrom().getFirstName();
                final SendMessage msg = new SendMessage();
                msg.setChatId(String.valueOf(update.getMessage().getChatId()));
                if (messageText.contains("#promo")) {
                    msg.setText(format("Hey there, %s %s", userName, promo));
                    result.add(msg);
                } else if (messageText.contains("#nextcommand")) {
                    msg.setText(format("Hey there, %s %s", userName, nextCommand));
                    result.add(msg);
                }
            }
        } catch (NullPointerException e) {
            log.info("Had a Nullpointer in CommandStrategy");
        }
        return result;
    }
}
