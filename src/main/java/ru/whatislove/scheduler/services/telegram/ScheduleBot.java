package ru.whatislove.scheduler.services.telegram;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.whatislove.scheduler.services.NotificationsService;
import ru.whatislove.scheduler.services.telegram.util.ManageEntity;

@Component
public class ScheduleBot extends TelegramLongPollingBot {

    private final String BOT_NAME;
    private final String BOT_TOKEN;
    private final CommandService commandService;
    private final NotificationsService notificationsService;

    @Autowired
    public ScheduleBot(@Value("${bot.name}") String botName, @Value("${bot.token}") String botToken,
                       CommandService commandService, NotificationsService notificationsService) {
        super();
        this.BOT_NAME = botName;
        this.BOT_TOKEN = botToken;
        this.notificationsService = notificationsService;
        TelegramBotsApi botsApi;
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        this.commandService = commandService;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    @Override
    public void onUpdateReceived(Update update) {
        List<ManageEntity> next;
        long chatId;
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            next = commandService.executeCommand(ManageEntity.builder().message(update.getCallbackQuery()
                    .getData()).build(), chatId);
        } else {
            var msg = update.getMessage();
            chatId = msg.getChatId();
            String txt = msg.getText() != null ? msg.getText() : msg.getCaption();
            next = commandService.executeCommand(ManageEntity.builder().message(txt).video(msg.getVideo())
                    .document(msg.getDocument()).sticker(msg.getSticker()).build(), chatId);
        }

        for (ManageEntity manageEntity : next) {
            try {
                sendMessage(manageEntity);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendMessage(ManageEntity manageEntity) throws TelegramApiException {

        try {
            if (manageEntity.getVideo() != null) {
                SendVideo sv = SendVideo.builder().chatId(manageEntity.getChatId())
                        .video(new InputFile(manageEntity.getVideo().getFileId())).build();
                execute(sv);
            }

            if (manageEntity.getDocument() != null) {
                SendDocument sd = SendDocument.builder().chatId(manageEntity.getChatId())
                        .document(new InputFile(manageEntity.getDocument().getFileId())).build();
                execute(sd);
            }

            if (manageEntity.getSticker() != null) {
                SendSticker sd = SendSticker.builder().chatId(manageEntity.getChatId())
                        .sticker(new InputFile(manageEntity.getSticker().getFileId())).build();
                execute(sd);
            }

            if (manageEntity.getMessage() != null) {
                SendMessage sm = SendMessage.builder().chatId(manageEntity.getChatId())
                        .text(manageEntity.getMessage())
                        .replyMarkup(manageEntity.getKeyboardMarkup()).build();
                execute(sm);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Async
    @Scheduled(cron = "0 * * * * *")
    void sendSchedule() {
        Map<Long, ManageEntity> messages = notificationsService.sendSchedule();

        if (messages == null)
            return;

        messages.forEach((user, msg) -> {
            try {
                sendMessage(msg);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
