package ru.whatislove.scheduler.services.telegram;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.whatislove.scheduler.models.*;
import ru.whatislove.scheduler.repository.*;
import ru.whatislove.scheduler.services.NotificationsService;
import ru.whatislove.scheduler.services.telegram.util.*;

@Service
public class TeacherCommandService {

    private final TeacherRepo teacherRepo;
    private final StudentRepo studentRepo;
    private final UserRepo userRepo;
    private final GroupRepo groupRepo;
    private final DisciplineRepo disciplineRepo;
    private final FileSaver fileSaver;
    private final ShowScheduleService showScheduleService;
    private final NotificationsService notificationsService;

    public TeacherCommandService(TeacherRepo teacherRepo, StudentRepo studentRepo, UserRepo userRepo, GroupRepo groupRepo, DisciplineRepo disciplineRepo, FileRepo fileRepo, FileSaver fileSaver, ShowScheduleService showScheduleService,
                                 NotificationsService notificationsService) {
        this.teacherRepo = teacherRepo;
        this.studentRepo = studentRepo;
        this.userRepo = userRepo;
        this.groupRepo = groupRepo;
        this.disciplineRepo = disciplineRepo;
        this.fileSaver = fileSaver;
        this.showScheduleService = showScheduleService;
        this.notificationsService = notificationsService;
    }

    public List<ManageEntity> teacherBranch(ManageEntity message, Teacher teacher, User user) {

        if (!user.getWaitCommand().isEmpty()) {
            return switch (Objects.requireNonNull(TeacherWaitCommand.fromString(user.getWaitCommand())).getCommand()) {
                case WAIT_KEY -> saveTeacherChatId(user, message.getMessage());
                case WAIT_TIME -> setNotificationsTime(message.getMessage(), user);
                case WAIT_GROUP_ID -> sendMessageGroup(teacher, user, message.getMessage());
                case WAIT_USER_ID -> setText(user, message.getMessage());
                case WAIT_TEXT_USER -> {
                    var userId = user.getWaitCommand().substring(14);
                    yield sendToUser(teacher, user, message, userId);
                }
                case WAIT_TEXT_GROUP -> {
                    var groupId = user.getWaitCommand().substring(15);
                    yield sendToGroup(teacher, user, message, groupId);
                }
            };
        }

        var command = Command.fromString(message.getMessage());

        return switch (command.getCommand()) {
            case NOTIFICATIONS -> enableNotifications(teacher);
            case TIME -> getNotificationsTimeExample(user);
            case CURRENT -> setCurrentNotifications(teacher, user.getChatId());
            case ADVANCE -> setAdvanceNotifications(teacher, user.getChatId());
            case SCHEDULE_TODAY -> scheduleToday(teacher, user.getChatId());
            case SCHEDULE_TOMORROW -> scheduleTomorrow(teacher, user.getChatId());
            case SCHEDULE_WEEK -> scheduleWeek(teacher, user.getChatId());
            case SEND_MESSAGE -> sendMessageMenu(teacher, user);
            case SHOW_FILES -> throw new RuntimeException();
        };
    }

    private List<ManageEntity> scheduleToday(Teacher teacher, long chatId) {
        String msg = showScheduleService.showTodaySchedule(userRepo.findByRoleIdAndRole(teacher.getId(),
                "teacher").get());
        return List.of(new ManageEntity(chatId, "Конечно, вот твое расписание на сегодня:", null),
                new ManageEntity(chatId, msg, null));
    }

    private List<ManageEntity> scheduleTomorrow(Teacher teacher, long chatId) {
        String msg = showScheduleService.showTomorrowSchedule(userRepo.findByRoleIdAndRole(teacher.getId(),
                "teacher").get());
        return List.of(new ManageEntity(chatId, "Конечно, вот твое расписание на завтра:", null),
                new ManageEntity(chatId, msg, null));
    }

    private List<ManageEntity> scheduleWeek(Teacher teacher, long chatId) {
        List<ManageEntity> result = new ArrayList<>(List.of(
                new ManageEntity(chatId, "Конечно, вот твое расписание на эту неделю:", null)));
        result.addAll(showScheduleService.showWeekSchedule(userRepo.findByRoleIdAndRole(teacher.getId(),
                        "teacher").get()).stream()
                .map(str -> new ManageEntity(chatId, str, null)).toList());
        return result;
    }

    private List<ManageEntity> enableNotifications(Teacher teacher) {
        return List.of(notificationsService.enableNotifications(userRepo.findByRoleIdAndRole(teacher.getId(),
                "teacher").get()));
    }

    private List<ManageEntity> getNotificationsTimeExample(User user) {
        user.setWaitCommand("/waitTime");
        userRepo.save(user);
        return List.of(
                new ManageEntity(user.getChatId(), """
                        Во сколько отправлять расписание?
                        Пример:\
                        """, null),
                new ManageEntity(user.getChatId(), "08:35", null));

    }

    private List<ManageEntity> setNotificationsTime(String time, User user) {
        if (notificationsService.setTime(user, time)) {
            return List.of(new ManageEntity(user.getChatId(), "Время успешно установлено", null));
        } else {
            return List.of(new ManageEntity(user.getChatId(), "Кажется, допущена ошибка в написании времени," +
                    " попробуй еще раз", null));
        }
    }

    private List<ManageEntity> setCurrentNotifications(Teacher teacher, long chatId) {
        notificationsService.setIsAdvance(userRepo.findByRoleIdAndRole(teacher.getId(),
                "teacher").get(), false);
        return List.of(new ManageEntity(chatId, "Теперь будет приходить расписание текущего дня", null));
    }

    private List<ManageEntity> setAdvanceNotifications(Teacher teacher, long chatId) {
        notificationsService.setIsAdvance(userRepo.findByRoleIdAndRole(teacher.getId(),
                "teacher").get(), true);
        return List.of(new ManageEntity(chatId, "Теперь будет приходить расписание следующего дня", null));
    }


    private List<ManageEntity> sendMessageMenu(Teacher teacher, User user) {
        var builder = InlineButtonsBuilder.builder();
        List<Discipline> disciplines = disciplineRepo.findAllByTeacherId(teacher.getId());
        var groups = groupRepo.findAllById(disciplines.stream().map(Discipline::getGroupId).distinct().toList());

        groups.forEach(g -> builder.addButton(InlineKeyboardButton.builder().text(g.getName())
                .callbackData(g.getId().toString()).build()));
        user.setWaitCommand("/waitGroupId");
        userRepo.save(user);

        return List.of(new ManageEntity(user.getChatId(), "Список групп, которым можно отправить сообщение",
                builder.build()));
    }

    private List<ManageEntity> sendMessageGroup(Teacher teacher, User user, String groupIdStr) {
        var builder = InlineButtonsBuilder.builder();
        long groupId = Long.parseLong(groupIdStr);
        var students = studentRepo.findAllByUniversityIdAndGroupId(teacher.getUniversityId(), groupId);

        builder.addSeparateButton(InlineKeyboardButton.builder().text("Вся группа").callbackData("/" + groupIdStr)
                .build());
        for (Student s : students) {
            builder.addButton(InlineKeyboardButton.builder().text(s.getName())
                    .callbackData(userRepo.findByRoleIdAndRole(s.getId(), "student").get().getRoleId().toString())
                    .build());
        }

        user.setWaitCommand("/waitUserId");
        userRepo.save(user);

        return List.of(new ManageEntity(user.getChatId(), "Кому отправим?",
                builder.build()));
    }

    private List<ManageEntity> setText(User user, String studentId) {

        if (studentId.contains("/")) {
            user.setWaitCommand("/waitTextGroup " + studentId.substring(1));
        } else {
            user.setWaitCommand("/waitTextUser " +
                    userRepo.findByRoleIdAndRole(Long.parseLong(studentId), "student")
                            .get().getChatId());
        }
        userRepo.save(user);

        return List.of(new ManageEntity(user.getChatId(), "Введите сообщение:", null));
    }

    private List<ManageEntity> sendToUser(Teacher teacher, User user, ManageEntity message, String userToSend) {

        fileSaver.saveItems(message, "user", userRepo.findByChatId(Long.parseLong(userToSend)).get().getId(),
                user.getId());
        message.setMessage(teacher.getName() + ":\n" + message.getMessage());
        var messages = new ArrayList<>(notificationsService.sendMessageToUser(message, Long.parseLong(userToSend)));
        user.setWaitCommand("");
        userRepo.save(user);
        messages.add(new ManageEntity(user.getChatId(), "Сообщение успешно отправлено",
                null));
        return messages;
    }

    private List<ManageEntity> sendToGroup(Teacher teacher, User user, ManageEntity message, String groupToSend) {

        fileSaver.saveItems(message, "group", Long.parseLong(groupToSend), user.getId());

        message.setMessage(teacher.getName() + ":\n" + message.getMessage());
        var messages = new ArrayList<>(notificationsService.sendMessageToGroup(message, groupToSend,
                teacher.getUniversityId()));
        user.setWaitCommand("");
        userRepo.save(user);

        messages.add(new ManageEntity(user.getChatId(), "Сообщение успешно отправлено",
                null));
        return messages;
    }

    public List<ManageEntity> registerTeacherCommand(User user) {
        return List.of(new ManageEntity(user.getChatId(),
                "Отлично, введите, пожалуйста, ваш идентификационный ключ:", null));
    }

    public List<ManageEntity> saveTeacherChatId(User user, String key) {

        var teacherOpt = teacherRepo.findTeacherByKey(key);

        if (teacherOpt.isEmpty()) {
            return List.of(new ManageEntity(user.getChatId(),
                    "Преподаватель с таким ключом не был найден, попробуйте еще раз", null));
        }

        var teacher = teacherOpt.get();
        user.setRoleId(teacher.getId());
        user.setWaitCommand("");
        userRepo.save(user);

        return List.of(new ManageEntity(user.getChatId(), "Здравствуйте, " + teacher.getName(), baseMenu));
    }

    private static final InlineKeyboardMarkup baseMenu = InlineButtonsBuilder.builder()
            .setChunk(1)
            .addButton(InlineKeyboardButton.builder()
                    .text("Включить/Выключить\nуведомления")
                    .callbackData("/notifications")
                    .build())
            .addButton(InlineKeyboardButton.builder()
                    .text("Установить время\nуведомлений")
                    .callbackData("/time")
                    .build())
            .addButton(InlineKeyboardButton.builder()
                    .text("Отправлять расписание день в день")
                    .callbackData("/current")
                    .build())
            .addButton(InlineKeyboardButton.builder()
                    .text("Отправлять расписание за день")
                    .callbackData("/advance")
                    .build())
            .setChunk(2)
            .addButton(InlineKeyboardButton.builder()
                    .text("Расписание на сегодня")
                    .callbackData("/schedule_today")
                    .build())
            .addButton(InlineKeyboardButton.builder()
                    .text("Расписание на завтра")
                    .callbackData("/schedule_tomorrow")
                    .build())
            .addButton(InlineKeyboardButton.builder()
                    .text("Расписание на неделю")
                    .callbackData("/schedule_week")
                    .build())
            .addButton(InlineKeyboardButton.builder()
                    .text("Отправить сообщение")
                    .callbackData("/send_message")
                    .build()).build();

}
