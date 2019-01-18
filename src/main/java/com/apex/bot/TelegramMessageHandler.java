package com.apex.bot;

import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramMessageHandler extends ATelegramBot {

    private static final String MP3_MIME = "audio/mp3";
    private static final String MP4_MIME = "video/mp4";
    private static final String MPEG_MIME = "video/mpeg";
    private static final String PDF_MIME= "application/pdf";
    private static final String PNG_MIME = "image/png";
    private static final String JPEG_MIME = "image/jpeg";
    private static final String GIF_MIME = "image/gif";
    private static final String MP3 = ".mp3";
    private static final String MPEG = ".mpeg";
    private static final String MP4 = ".mp4";
    private static final String PDF= ".pdf";
    private static final String PNG = ".png";
    private static final String JPEG = ".jpeg";
    private static final String GIF = ".gif";

    public TelegramMessageHandler(String token, String botname) {
        super(token, botname);
    }

    @Override
    public void onUpdateReceived(Update update) {
        int id = update.getMessage().getMessageId();
        if(update.getMessage().hasDocument()){
            Document doc  = update.getMessage().getDocument();
            if(!(doc.getMimeType().equals(MP3_MIME) || doc.getMimeType().equals(MP4_MIME) || doc.getMimeType().equals(MPEG_MIME)
                    || doc.getMimeType().equals(PDF_MIME) || doc.getMimeType().equals(PNG_MIME) || doc.getMimeType().equals(JPEG_MIME)
                    || doc.getMimeType().equals(GIF_MIME)) &&
                    !(doc.getFileName().endsWith(MP3) || doc.getFileName().endsWith(MP4) || doc.getFileName().endsWith(MPEG)
                    || doc.getFileName().endsWith(PDF) || doc.getFileName().endsWith(PNG) || doc.getFileName().endsWith(JPEG)
                    || doc.getFileName().endsWith(GIF))){
                log.info(String.valueOf(id));
                DeleteMessage delete = new DeleteMessage(update.getMessage().getChatId(), id);
                try {
                    execute(delete);
                } catch (TelegramApiException e) {
                    log.error("Failed to delete message" + e.getMessage());
                }
            }
        }
    }
}
