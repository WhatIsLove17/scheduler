package ru.whatislove.scheduler.services.telegram.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ManageEntity {
    private long chatId;
    private String message;
    private Video video;
    private Document document;
    private Sticker sticker;
    private List<PhotoSize> photos;
    private ReplyKeyboard keyboardMarkup;

    public ManageEntity(long chatId, String message, ReplyKeyboard keyboardMarkup) {
        this.chatId = chatId;
        this.message = message;
        this.keyboardMarkup = keyboardMarkup;
    }
}
