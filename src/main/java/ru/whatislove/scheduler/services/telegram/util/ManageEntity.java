package ru.whatislove.scheduler.services.telegram.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

@Data
@AllArgsConstructor
public class ManageEntity {
    private long chatId;
    private String message;
    private ReplyKeyboard keyboardMarkup;
}
