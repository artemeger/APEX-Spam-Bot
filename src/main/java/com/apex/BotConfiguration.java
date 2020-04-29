package com.apex;

import com.apex.bot.TelegramSessionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotConfiguration {

    @Bean
    public TelegramSessionManager getTelegramSessionManager(){
        return new TelegramSessionManager();
    }

}
