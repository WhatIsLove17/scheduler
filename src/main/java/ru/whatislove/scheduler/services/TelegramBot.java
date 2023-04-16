package ru.whatislove.scheduler.services;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.whatislove.scheduler.config.BotConfig;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;

    private final MessageService messageService;


    public TelegramBot(BotConfig botConfig, MessageService messageService) {
        this.botConfig = botConfig;
        this.messageService = messageService;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        /*Message message = update.getMessage();

        String answer = messageService.createMessage(message.getText(), message.getChatId());

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText(answer);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

*/
    }
}
