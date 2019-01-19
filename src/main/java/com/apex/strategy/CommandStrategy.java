package com.apex.strategy;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

public class CommandStrategy implements IStrategy {

    @Override
    public Optional<BotApiMethod> runStrategy(Update update) {
        if(update.hasMessage()) {
            String messageText = update.getMessage().getText();
            if(messageText.contains("!forgive")){
               int userId = update.getMessage().getReplyToMessage().getFrom().getId();
                DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                ConcurrentMap map = database.hashMap("user").createOrOpen();
                map.remove(userId);
                database.close();
            }
        }
        return Optional.empty();
    }
}
