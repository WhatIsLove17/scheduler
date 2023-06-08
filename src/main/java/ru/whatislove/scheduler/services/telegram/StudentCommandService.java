package ru.whatislove.scheduler.services.telegram;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker;
import ru.whatislove.scheduler.models.*;
import ru.whatislove.scheduler.repository.*;
import ru.whatislove.scheduler.services.NotificationsService;
import ru.whatislove.scheduler.services.telegram.util.*;

@Service
public class StudentCommandService {

    private final List<University> universities;
    private final List<String> cities;
    private final GroupRepo groupRepo;
    private final FileRepo fileRepo;
    private final TeacherRepo teacherRepo;
    private final FileSaver fileSaver;
    private final StudentRepo studentRepo;
    private final DisciplineRepo disciplineRepo;
    private final UserRepo userRepo;
    private final ShowScheduleService showScheduleService;
    private final NotificationsService notificationsService;


    public StudentCommandService(UniversityRepo universityRepo, GroupRepo groupRepo, FileRepo fileRepo, TeacherRepo teacherRepo, FileSaver fileSaver, StudentRepo studentRepo,
                                 DisciplineRepo disciplineRepo, UserRepo userRepo, ShowScheduleService showScheduleService, NotificationsService notificationsService) {
        universities = Lists.newArrayList(universityRepo.findAll().iterator());
        this.groupRepo = groupRepo;
        this.fileRepo = fileRepo;
        this.teacherRepo = teacherRepo;
        this.fileSaver = fileSaver;
        this.studentRepo = studentRepo;
        this.disciplineRepo = disciplineRepo;
        this.userRepo = userRepo;
        this.showScheduleService = showScheduleService;
        this.notificationsService = notificationsService;
        cities = universities.stream().map(University::getCity).distinct().collect(Collectors.toList());
    }

    public List<ManageEntity> studentBranch(ManageEntity command, Student student, User user) {

        var commandWrapper = Command.fromString(command.getMessage());

        if (!user.getWaitCommand().isEmpty()) {
            return switch (Objects.requireNonNull(StudentWaitCommand.fromString(user.getWaitCommand())).getCommand()) {
                case WAIT_CITY -> setCity(command.getMessage(), student, user);
                case WAIT_UNIVERSITY -> setUniversity(command.getMessage(), student, user);
                case WAIT_FACULTY -> setFaculty(command.getMessage(), student, user);
                case WAIT_YEAR -> setYear(command.getMessage(), student, user);
                case WAIT_GROUP -> setGroup(command.getMessage(), student, user);
                case WAIT_TIME -> setNotificationsTime(command.getMessage(), user);
                case WAIT_NAME -> setName(command.getMessage(), student, user);
                case WAIT_TEXT_USER -> sendToUser(student, user, command, user.getWaitCommand().substring(14));
                case WAIT_TEACHER_ID -> setText(user, command.getMessage());
                case WAIT_RESOURCE -> showFiles(student, user, command.getMessage());
            };
        }

        return switch (commandWrapper.getCommand()) {
            case NOTIFICATIONS -> enableNotifications(student);
            case CURRENT -> setCurrentNotifications(student, user);
            case ADVANCE -> setAdvanceNotifications(user);
            case SCHEDULE_TODAY -> scheduleToday(user);
            case SCHEDULE_TOMORROW -> scheduleTomorrow(user);
            case SCHEDULE_WEEK -> scheduleWeek(user);
            case SEND_MESSAGE -> sendMessageMenu(student, user);
            case TIME -> getNotificationsTimeExample(user);
            case SHOW_FILES -> filesMenu(student, user);
        };
    }

    public List<ManageEntity> registerStudentCommand(User user) {
        var manageEntityList = new ArrayList<ManageEntity>();

        var bBuilder = InlineButtonsBuilder.builder();
        for (String city : cities) {
            bBuilder.addButton(InlineKeyboardButton.builder().callbackData(city).text(city).build());
        }

        manageEntityList.add(new ManageEntity(user.getChatId(), "Из какого ты города?", bBuilder.build()));
        return manageEntityList;
    }

    private List<ManageEntity> setCity(String city, Student student, User user) {
        var bBuilder = InlineButtonsBuilder.builder();
        universities.stream().filter(u -> u.getCity().equals(city)).forEach(u ->
                bBuilder.addButton(InlineKeyboardButton.builder().callbackData(u.getName()).text(u.getName()).build()));

        if (!cities.contains(city)) {
            throw new IllegalArgumentException();
        }

        student.setCity(city);
        studentRepo.save(student);
        user.setWaitCommand("/waitUniversity");
        userRepo.save(user);

        return List.of(new ManageEntity(user.getChatId(), "Отлично, где ты учишься?", bBuilder.build()));
    }

    private List<ManageEntity> setUniversity(String university, Student student, User user) {
        long universityId = universities.stream().filter(u -> u.getName().equals(university)).findFirst().get().getId();

        List<Group> groups = groupRepo.findAllByUniversityId(universityId);

        var bBuilder = InlineButtonsBuilder.builder();
        groups.stream().map(Group::getFaculty).distinct().forEach(f -> bBuilder.addButton(InlineKeyboardButton.builder()
                .callbackData(f).text(f).build()));

        student.setUniversityId(universityId);
        studentRepo.save(student);
        user.setWaitCommand("/waitFaculty");
        userRepo.save(user);

        return List.of(new ManageEntity(user.getChatId(), "Определим твой факультет?...Слизерин там, Грифиндор",
                bBuilder.build()));
    }

    private List<ManageEntity> setFaculty(String faculty, Student student, User user) {

        long universityId = universities.stream().filter(u -> u.getId().equals(student.getUniversityId()))
                .findFirst().get().getId();

        var bBuilder = InlineButtonsBuilder.builder();

        groupRepo.findAllByUniversityIdAndFaculty(universityId, faculty).stream().map(Group::getYear).distinct()
                .sorted(Integer::compareTo).forEach(year -> bBuilder.addButton(InlineKeyboardButton.builder()
                        .callbackData(Integer.toString(year)).text(Integer.toString(year)).build()));

        student.setFaculty(faculty);
        studentRepo.save(student);
        user.setWaitCommand("/waitYear");
        userRepo.save(user);

        return List.of(new ManageEntity(user.getChatId(), "Какой курс?", bBuilder.build()));
    }

    private List<ManageEntity> setYear(String year, Student student, User user) {
        long universityId = universities.stream().filter(u -> u.getId().equals(student.getUniversityId()))
                .findFirst().get().getId();

        var bBuilder = InlineButtonsBuilder.builder();

        groupRepo.findAllByUniversityIdAndFaculty(universityId, student.getFaculty()).stream()
                .filter(g -> g.getYear() == Integer.parseInt(year)).map(Group::getName).sorted()
                .forEach(g -> bBuilder.addButton(InlineKeyboardButton.builder().callbackData(g).text(g).build()));

        student.setYear(Integer.parseInt(year));
        studentRepo.save(student);
        user.setWaitCommand("/waitGroup");
        userRepo.save(user);

        return List.of(new ManageEntity(user.getChatId(), "Какая группа?", bBuilder.build()));
    }

    private List<ManageEntity> setGroup(String group, Student student, User user) {
        long universityId = universities.stream().filter(u -> u.getId().equals(student.getUniversityId()))
                .findFirst().get().getId();

        long groupId = groupRepo.findAllByUniversityIdAndFaculty(universityId, student.getFaculty()).stream()
                .filter(g -> g.getName().equals(group)).findFirst().get().getId();

        student.setGroupId(groupId);
        studentRepo.save(student);
        user.setWaitCommand("/waitName");
        userRepo.save(user);

        return List.of(new ManageEntity(user.getChatId(), "Введи свое ФИО?\n" +
                "Так преподавателям будет проще с тобой коммуницировать", null));
    }

    private List<ManageEntity> setName(String name, Student student, User user) {
        student.setName(name);
        studentRepo.save(student);
        user.setWaitCommand("");
        userRepo.save(user);

        return List.of(new ManageEntity(user.getChatId(), "Отлично, я готов работе!\n" +
                "Вот список того, что я умею:", baseMenu));
    }

    private List<ManageEntity> scheduleToday(User user) {
        String msg = showScheduleService.showTodaySchedule(user);
        return List.of(new ManageEntity(user.getChatId(), "Конечно, вот твое расписание на сегодня:", null),
                new ManageEntity(user.getChatId(), msg, null));
    }

    private List<ManageEntity> scheduleTomorrow(User user) {
        String msg = showScheduleService.showTomorrowSchedule(user);
        return List.of(new ManageEntity(user.getChatId(), "Конечно, вот твое расписание на завтра:", null),
                new ManageEntity(user.getChatId(), msg, null));
    }

    private List<ManageEntity> scheduleWeek(User user) {
        List<ManageEntity> result = new ArrayList<>(List.of(
                new ManageEntity(user.getChatId(), "Конечно, вот твое расписание на эту неделю:", null)));
        result.addAll(showScheduleService.showWeekSchedule(user).stream()
                .map(str -> new ManageEntity(user.getChatId(), str, null)).toList());
        return result;
    }

    private List<ManageEntity> enableNotifications(Student student) {
        return List.of(notificationsService.enableNotifications(userRepo.findByRoleIdAndRole(student.getId(),
                "student").get()));
    }

    private List<ManageEntity> sendMessageMenu(Student student, User user) {
        var builder = InlineButtonsBuilder.builder();
        List<Discipline> disciplines = disciplineRepo.findAllByGroupId(student.getGroupId());
        List<Teacher> teachers = disciplines.stream().filter(d -> d.getTeacherId() != null)
                .map(d -> teacherRepo.findById(d.getTeacherId()).get())
                .distinct().toList();

        teachers.forEach(t -> builder.addSeparateButton(InlineKeyboardButton.builder().text(t.getName())
                .callbackData(t.getId().toString()).build()));

        user.setWaitCommand("/waitTeacherId");
        userRepo.save(user);

        return List.of(new ManageEntity(user.getChatId(), "Список преподавателей,\nкоторым можно отправить сообщение",
                builder.build()));
    }

    private List<ManageEntity> setText(User user, String teacherId) {

        user.setWaitCommand("/waitTextUser " +
                userRepo.findByRoleIdAndRole(Long.parseLong(teacherId), "teacher")
                        .get().getChatId());
        userRepo.save(user);

        return List.of(new ManageEntity(user.getChatId(), "Введите сообщение:", null));
    }

    private List<ManageEntity> sendToUser(Student student, User user, ManageEntity message, String userToSend) {

        fileSaver.saveItems(message, "user", userRepo.findByChatId(Long.parseLong(userToSend)).get().getId(),
                user.getId());
        message.setMessage(student.getName() + ":\n" + (message.getMessage() == null ? "" : message.getMessage()));
        var messages = new ArrayList<>(notificationsService.sendMessageToUser(message, Long.parseLong(userToSend)));
        user.setWaitCommand("");
        userRepo.save(user);
        messages.add(new ManageEntity(user.getChatId(), "Сообщение успешно отправлено",
                null));
        return messages;
    }

    private List<ManageEntity> filesMenu(Student student, User user) {

        var builder = InlineButtonsBuilder.builder();

        var files = fileRepo.findAllByReceiverIdAndReceiverType(user.getId(), "user");
        files.addAll(fileRepo.findAllByReceiverIdAndReceiverType(student.getGroupId(), "group"));
        
        files.stream().filter(Objects::nonNull).map(f -> userRepo.findById(f.getSenderId()).get()).distinct()
                .filter(s -> s.getRole().equals("teacher")).map(s -> teacherRepo.findById(s.getRoleId()).get())
                .forEach(t -> builder.addSeparateButton(InlineKeyboardButton.builder().callbackData(t.getId().toString())
                        .text(t.getName()).build()));
        user.setWaitCommand("/waitSource");
        userRepo.save(user);

        return List.of(new ManageEntity(user.getChatId(), "Ресурсы из какого источника?", builder.build()));
    }

    private List<ManageEntity> showFiles(Student student, User user, String teacherId) {

        var files = fileRepo.findAllByReceiverIdAndReceiverType(user.getId(), "user");
        files.addAll(fileRepo.findAllByReceiverIdAndReceiverType(student.getGroupId(), "group"));

        var result = files.stream().filter(f -> userRepo.findById(f.getSenderId()).get().getRoleId() == Long.parseLong(teacherId))
                        .map(f -> {
                            var manageEntity = new ManageEntity(user.getChatId(), null, null);
                            if (f.getType().equals("doc")) {
                                manageEntity.setDocument(new Document(f.getFileId(), null, null,
                                        null, null, null));
                            }
                            if (f.getType().equals("video")) {
                                manageEntity.setVideo(new Video(f.getFileId(), null, null, null, null, null, null,
                                        null, null));
                            }
                            if (f.getType().equals("sticker")) {
                                manageEntity.setSticker(new Sticker(f.getFileId(), null, null, null, null, null, null,
                                        null, null, null, null, null));
                            }
                            return manageEntity;

                        }).collect(Collectors.toList());
        user.setWaitCommand("");
        userRepo.save(user);

        return result;
    }

    private List<ManageEntity> setCurrentNotifications(Student student, User user) {
        notificationsService.setIsAdvance(user, false);
        return List.of(new ManageEntity(user.getChatId(), "Теперь будет приходить расписание текущего дня", null));
    }

    private List<ManageEntity> setAdvanceNotifications(User user) {
        notificationsService.setIsAdvance(user, true);
        return List.of(new ManageEntity(user.getChatId(), "Теперь будет приходить расписание следующего дня", null));
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
            user.setWaitCommand("");
            userRepo.save(user);
            return List.of(new ManageEntity(user.getChatId(), "Время успешно установлено", null));
        } else {
            return List.of(new ManageEntity(user.getChatId(), "Кажется, допущена ошибка в написании времени," +
                    " попробуй еще раз", null));
        }
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
                    .build())
            .addButton(InlineKeyboardButton.builder()
                    .text("Открыть файлы")
                    .callbackData("/show_files")
                    .build()).build();
}
