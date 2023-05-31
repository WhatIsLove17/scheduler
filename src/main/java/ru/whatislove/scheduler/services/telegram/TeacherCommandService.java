package ru.whatislove.scheduler.services.telegram;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import ru.whatislove.scheduler.models.Teacher;
import ru.whatislove.scheduler.repository.TeacherRepo;
import ru.whatislove.scheduler.services.NotificationsService;
import ru.whatislove.scheduler.services.telegram.util.Command;
import ru.whatislove.scheduler.services.telegram.util.ManageEntity;

@Service
public class TeacherCommandService {

    private final TeacherRepo teacherRepo;

    private final ShowScheduleService showScheduleService;

    private final NotificationsService notificationsService;

    public TeacherCommandService(TeacherRepo teacherRepo, ShowScheduleService showScheduleService,
                                 NotificationsService notificationsService) {
        this.teacherRepo = teacherRepo;
        this.showScheduleService = showScheduleService;
        this.notificationsService = notificationsService;
    }

    public List<ManageEntity> teacherBranch(String command, Teacher teacher) {

        var commandWrapper = Command.fromString(command);

        return switch (commandWrapper.getCommand()) {
            case NOTIFICATIONS -> enableNotifications(teacher);
            case TIME -> setNotificationsTime(teacher, commandWrapper.getMsg().get(0));
            case CURRENT -> setCurrentNotifications(teacher);
            case ADVANCE -> setAdvanceNotifications(teacher);
            case SCHEDULE_TODAY -> scheduleToday(teacher);
            case SCHEDULE_TOMORROW -> scheduleTomorrow(teacher);
            case SCHEDULE_WEEK -> scheduleWeek(teacher);
            case SEND_MESSAGE -> sendMessage(teacher, commandWrapper.getMsg());
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };
    }

    private List<ManageEntity> scheduleToday(Teacher teacher) {
        String msg = showScheduleService.showTodaySchedule(null, teacher);
        return List.of(new ManageEntity(teacher.getChatId(), "Конечно, вот твое расписание на сегодня:", null),
                new ManageEntity(teacher.getChatId(), msg, null));
    }

    private List<ManageEntity> scheduleTomorrow(Teacher teacher) {
        String msg = showScheduleService.showTomorrowSchedule(null, teacher);
        return List.of(new ManageEntity(teacher.getChatId(), "Конечно, вот твое расписание на завтра:", null),
                new ManageEntity(teacher.getChatId(), msg, null));
    }

    private List<ManageEntity> scheduleWeek(Teacher teacher) {
        List<ManageEntity> result = new ArrayList<>(List.of(
                new ManageEntity(teacher.getChatId(), "Конечно, вот твое расписание на эту неделю:", null)));
        result.addAll(showScheduleService.showWeekSchedule(null, teacher).stream()
                .map(str -> new ManageEntity(teacher.getChatId(), str, null)).toList());
        return result;
    }

    private List<ManageEntity> enableNotifications(Teacher teacher) {
        return List.of(notificationsService.enableNotifications(null, teacher));
    }

    private List<ManageEntity> setNotificationsTime(Teacher teacher, String time) {
        if (time.isEmpty()) {
            return List.of(
                    new ManageEntity(teacher.getChatId(), """
                            Чтобы установить время рассылки, дополни команду
                            Пример:\
                            """, null),
                    new ManageEntity(teacher.getChatId(), "/time 08:35", null));
        } else {
            if (notificationsService.setTime(null, teacher, time)) {
                return List.of(new ManageEntity(teacher.getChatId(), "Время успешно установлено", null));
            } else {
                return List.of(new ManageEntity(teacher.getChatId(), "Кажется, допущена ошибка в написании времени," +
                        " попробуй еще раз", null));
            }
        }
    }

    private List<ManageEntity> setCurrentNotifications(Teacher teacher) {
        notificationsService.setIsAdvance(null, teacher, false);
        return List.of(new ManageEntity(teacher.getChatId(), "Теперь будет приходить расписание текущего дня", null));
    }

    private List<ManageEntity> setAdvanceNotifications(Teacher teacher) {
        notificationsService.setIsAdvance(null, teacher, true);
        return List.of(new ManageEntity(teacher.getChatId(), "Теперь будет приходить расписание следующего дня", null));
    }

    private List<ManageEntity> sendMessage(Teacher teacher, List<String> command) {
        if (command.isEmpty()) {
            return List.of(
                    new ManageEntity(teacher.getChatId(), """
                            Чтобы отправить сообщение группе, дополните команду номером группы и сообщением
                            Пример:\
                            """, null),
                    new ManageEntity(teacher.getChatId(), "/send_message 9301 Сегодня вашей пары не будет", null));
        } else {
            var messages = notificationsService.sendMessage(String.join(" ", command.subList(1, command.size())),
                    command.get(0), teacher.getUniversityId());
            messages.add(new ManageEntity(teacher.getChatId(),
                    "Сообщение успешно отпарвлено", null));
            return messages;
        }
    }

    public List<ManageEntity> registerTeacherCommand(long chatId) {
        return List.of(
                new ManageEntity(chatId,
                        "Отлично, введите, пожалуйста, ваш идентификационный ключ:", null));
    }

    public List<ManageEntity> saveTeacherChatId(long chatId, String key) {

        var teacherOpt = teacherRepo.findTeacherByKey(key);

        if (teacherOpt.isEmpty()) {
            return List.of(new ManageEntity(chatId, "Преподаватель с таким ключом не был найден, попробуйте еще раз",
                    null));
        }

        var teacher = teacherOpt.get();
        teacher.setChatId(chatId);

        return List.of(new ManageEntity(teacher.getChatId(), "Здравствуйте, " + teacher.getName(), null),
                new ManageEntity(teacher.getChatId(), """
                         * /notifications - включить/выключить ежедневную рассылку расписания
                         * /time - установить время рассылки (07:00 по умолчанию)
                         * /current - в рассылке расписание текущего дня
                         * /advance - в рассылке расписание следующего дня
                         * /schedule_today - расписание на сегодня
                         * /schedule_tomorrow - расписание на завтра
                         * /schedule_week - расписание на неделю
                         * /send_message - отправить сообщение студентам группы\
                        """, null));
    }

}
