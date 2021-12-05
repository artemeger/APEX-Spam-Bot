package com.apex;

import com.apex.bot.TelegramSessionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Configuration
public class BotConfiguration {

    @Bean
    public TelegramSessionManager getTelegramSessionManager() throws TelegramApiException {
        return new TelegramSessionManager();
    }

}
