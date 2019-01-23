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

import com.apex.bot.TelegramMessageHandler;
import com.apex.objects.Feedback;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class DeleteFileStrategy implements IStrategy {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String MP3_MIME = "audio/mp3";
    private static final String MP4_MIME = "video/mp4";
    private static final String MPEG_MIME = "video/mpeg";
    private static final String PDF_MIME= "application/pdf";
    private static final String PNG_MIME = "image/png";
    private static final String JPEG_MIME = "image/jpeg";
    private static final String GIF_MIME = "image/gif";
    private static final String TXT_MIME = "text/plain";
    private static final String MP3 = ".mp3";
    private static final String MPEG = ".mpeg";
    private static final String MP4 = ".mp4";
    private static final String PDF= ".pdf";
    private static final String PNG = ".png";
    private static final String JPEG = ".jpeg";
    private static final String JPG = ".jpg";
    private static final String GIF = ".gif";
    private static final String TXT = ".txt";

    @Override
    public ArrayList<Optional<BotApiMethod>> runStrategy(Update update) {
        ArrayList<Optional<BotApiMethod>> result = new ArrayList<>();
        result.add(Optional.empty());
        Document doc = update.getMessage().getDocument();
        String mimeName = doc.getMimeType();
        String fileName = doc.getFileName();
        if (!(mimeName.equals(MP3_MIME) || mimeName.equals(MP4_MIME) || mimeName.equals(MPEG_MIME)
                || mimeName.equals(PDF_MIME) || mimeName.equals(PNG_MIME) || mimeName.equals(JPEG_MIME)
                || mimeName.equals(GIF_MIME) || mimeName.equals(TXT_MIME)) || !(fileName.endsWith(MP3)
                || fileName.endsWith(MP4) || fileName.endsWith(MPEG) || fileName.endsWith(PDF)
                || fileName.endsWith(PNG) || fileName.endsWith(JPEG) || fileName.endsWith(GIF)
                || fileName.endsWith(JPG) || fileName.endsWith(TXT))) {
            result.add(Optional.of(new ForwardMessage(TelegramMessageHandler.VERIFICATON, update.getMessage().getChatId(), update.getMessage().getMessageId())));
            result.add(sendBanKeyboard(update.getMessage().getFrom().getId(), update.getMessage().getChatId()));
            result.add(Optional.of(new DeleteMessage(update.getMessage().getChatId(), update.getMessage().getMessageId())));
        }
        return result;
    }

    private Optional<BotApiMethod> sendBanKeyboard(int userId, long chatid) {
        SendMessage message = new SendMessage();
        message.setChatId(TelegramMessageHandler.VERIFICATON);
        message.setText("User shared this file. Ban?");
        DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
        ConcurrentMap map = database.hashMap("feedback").createOrOpen();
        String id = UUID.randomUUID().toString();
        map.put(id, new Feedback("", userId, chatid));
        database.close();
        try {
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText("Yes");
            button1.setCallbackData("ban,"+id);
            row1.add(button1);
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton button2 = new InlineKeyboardButton();
            button2.setText("No");
            button2.setCallbackData("false");
            row2.add(button2);
            keyboard.add(row1);
            keyboard.add(row2);
            inlineKeyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(inlineKeyboardMarkup);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return Optional.of(message);
    }
}
