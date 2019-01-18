package com.apex.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.meta.generics.LongPollingBot;

public class TelegramSessionManager implements IRunWithOwnThread{

    private TelegramBotsApi botsApi;
    private BotSession session;
    private Logger log = LoggerFactory.getLogger(this.getClass());

    public TelegramSessionManager(){
        ApiContextInitializer.init();
        this.botsApi = new TelegramBotsApi();
    }

    public void addPollingBot(LongPollingBot bot){
        try {
            session = this.botsApi.registerBot(bot);
        } catch (TelegramApiRequestException e) {
            log.error("Telegram Api bot registration failed", e.getApiResponse());
        }
    }

    @Override
    public void start() {
        if(session != null && !session.isRunning()){
            session.start();
        }
    }

    @Override
    public void stop() {
        session.stop();
    }

}
