package ru.whatislove.scheduler.services.telegram.util;

import java.util.ArrayList;
import java.util.List;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

public class InlineButtonsBuilder {

    private InlineButtonsBuilder() {
        buttons = new ArrayList<>();
        keyboardRows = new ArrayList<>();
    }

    private List<InlineKeyboardButton> buttons;
    private List<List<InlineKeyboardButton>> keyboardRows;
    private int chunk = 4;

    public static InlineButtonsBuilder builder() {
        return new InlineButtonsBuilder();
    }

    public InlineButtonsBuilder addButton(InlineKeyboardButton button) {

        buttons.add(button);
        if (buttons.size() == 4) {
            keyboardRows.add(buttons);
            buttons = new ArrayList<>();
        }

        return this;
    }

    public InlineButtonsBuilder addSeparateButton(InlineKeyboardButton button) {
        keyboardRows.add(List.of(button));
        return this;
    }

    public InlineKeyboardMarkup build() {
        return InlineKeyboardMarkup.builder().keyboard(keyboardRows).build();
    }

}
