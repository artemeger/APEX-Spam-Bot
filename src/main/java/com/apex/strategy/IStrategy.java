package com.apex.strategy;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@FunctionalInterface
public interface IStrategy {
    Optional<BotApiMethod> runStrategy(Update update);
}
