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
        } else return Optional.empty();
    }
}
