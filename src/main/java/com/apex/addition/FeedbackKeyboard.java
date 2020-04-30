package com.apex.addition;

import com.apex.entities.Feedback;
import com.apex.repository.IFeedbackRepository;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

public class FeedbackKeyboard {

    private final IFeedbackRepository feedbackRepository;

    private final int userId;

    private final long chatId;

    private final long verification;

    private final String data;

    public FeedbackKeyboard(final int userId, final long chatId, final long verification,
                            final String data, final IFeedbackRepository feedbackRepository){
        this.feedbackRepository = feedbackRepository;
        this.userId = userId;
        this.chatId = chatId;
        this.verification = verification;
        this.data = data;
    }

    public BotApiMethod getBanKeyboard() {
        SendMessage message = new SendMessage();
        message.setChatId(verification);
        message.setText("This Post was shared");

        feedbackRepository.save(new Feedback(userId, chatId, data));
        final Optional<Feedback> feedbackOpt = feedbackRepository.findFirstByUserId(userId);
        long id = 0L;
        if(feedbackOpt.isPresent()){
            id = feedbackOpt.get().getFeedbackId();
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("Blacklist content and ban");
        button1.setCallbackData(FeedbackAction.BAN.getAction() + "," + id);
        row1.add(button1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("Add User to whitelist");
        button2.setCallbackData(FeedbackAction.WHITELIST.getAction() + "," + id);
        row2.add(button2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText("Ignore");
        button3.setCallbackData(FeedbackAction.IGNORE.getAction());
        row3.add(button3);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        inlineKeyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(inlineKeyboardMarkup);

        return message;
    }

}
