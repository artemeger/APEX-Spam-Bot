package com.apex.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public abstract class ATelegramBot extends TelegramLongPollingBot {

    ATelegramBot(String token, String botname){
        this.token = token;
        this.botname = botname;

    }

    String token;
    String botname;
    Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public abstract void onUpdateReceived(Update update);

    @Override
    public String getBotUsername() {
        return botname;
    }

    @Override
    public String getBotToken() {
        return token;
    }

}
