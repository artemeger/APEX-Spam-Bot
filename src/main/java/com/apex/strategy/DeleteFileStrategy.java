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

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.util.Optional;

public class DeleteFileStrategy implements IStrategy {

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
    public Optional<BotApiMethod> runStrategy(Update update) {
        Document doc = update.getMessage().getDocument();
        String mimeName = doc.getMimeType();
        String fileName = doc.getFileName();
        if (!(mimeName.equals(MP3_MIME) || mimeName.equals(MP4_MIME) || mimeName.equals(MPEG_MIME)
                || mimeName.equals(PDF_MIME) || mimeName.equals(PNG_MIME) || mimeName.equals(JPEG_MIME)
                || mimeName.equals(GIF_MIME) || mimeName.equals(TXT_MIME)) || !(fileName.endsWith(MP3)
                || fileName.endsWith(MP4) || fileName.endsWith(MPEG) || fileName.endsWith(PDF)
                || fileName.endsWith(PNG) || fileName.endsWith(JPEG) || fileName.endsWith(GIF)
                || fileName.endsWith(JPG) || fileName.endsWith(TXT))) {
            return Optional.of(new DeleteMessage(update.getMessage().getChatId(), update.getMessage().getMessageId()));
        }
        return Optional.empty();
    }
}
