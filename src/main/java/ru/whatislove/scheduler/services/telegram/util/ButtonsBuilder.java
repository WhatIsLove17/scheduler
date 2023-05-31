package ru.whatislove.scheduler.services.telegram.util;

import java.util.ArrayList;
import java.util.List;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

public class ButtonsBuilder {

    private ButtonsBuilder() {
        buttons = new ArrayList<>();
        keyboardRows = new ArrayList<>();
    }

    private List<KeyboardButton> buttons;
    private List<KeyboardRow> keyboardRows;
    private int chunk = 4;

    public static ButtonsBuilder builder() {
        return new ButtonsBuilder();
    }

    public ButtonsBuilder addButton(KeyboardButton button) {

        buttons.add(button);
        if (buttons.size() == 4) {
            keyboardRows.add(new KeyboardRow(buttons));
            buttons.clear();
        }

        return this;
    }

    public ButtonsBuilder addSeparateButton(KeyboardButton button) {
        keyboardRows.add(new KeyboardRow(List.of(button)));
        return this;
    }

    public ReplyKeyboardMarkup build() {
        keyboardRows.add(new KeyboardRow(buttons));
        return ReplyKeyboardMarkup.builder().keyboard(keyboardRows).oneTimeKeyboard(true).build();
    }

}
