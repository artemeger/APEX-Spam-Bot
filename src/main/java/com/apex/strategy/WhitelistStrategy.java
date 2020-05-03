package com.apex.strategy;

import com.apex.entities.TGUser;
import com.apex.repository.ITGUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;

@Component
public class WhitelistStrategy implements IStrategy {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ITGUserRepository tgUserRepository;

    @Override
    public ArrayList<BotApiMethod> runStrategy(Update update) {
        final User userToWhitelist = update.getMessage().getForwardFrom();
        if(userToWhitelist != null) {
            final int userId = userToWhitelist.getId();
            tgUserRepository.findById(userId).ifPresentOrElse(
                    user -> log.info("User is already known. Ignore"),
                    () -> {
                        tgUserRepository.save(new TGUser(userToWhitelist.getId(), 0, true));
                        log.info("Whitelisted user " + userToWhitelist.getId());
                    });
        }
        return new ArrayList<>();
    }

}
