package ru.whatislove.scheduler.services.telegram;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import ru.whatislove.scheduler.models.Group;
import ru.whatislove.scheduler.models.Student;
import ru.whatislove.scheduler.models.University;
import ru.whatislove.scheduler.repository.GroupRepo;
import ru.whatislove.scheduler.repository.StudentRepo;
import ru.whatislove.scheduler.repository.UniversityRepo;
import ru.whatislove.scheduler.services.NotificationsService;
import ru.whatislove.scheduler.services.telegram.util.ButtonsBuilder;
import ru.whatislove.scheduler.services.telegram.util.Command;
import ru.whatislove.scheduler.services.telegram.util.ManageEntity;

@Service
public class StudentCommandService {

    private final List<University> universities;
    private final List<String> cities;
    private final GroupRepo groupRepo;
    private final StudentRepo studentRepo;
    private final ShowScheduleService showScheduleService;
    private final NotificationsService notificationsService;


    public StudentCommandService(UniversityRepo universityRepo, GroupRepo groupRepo, StudentRepo studentRepo,
                                 ShowScheduleService showScheduleService, NotificationsService notificationsService) {
        universities = Lists.newArrayList(universityRepo.findAll().iterator());
        this.groupRepo = groupRepo;
        this.studentRepo = studentRepo;
        this.showScheduleService = showScheduleService;
        this.notificationsService = notificationsService;
        cities = universities.stream().map(University::getCity).distinct().collect(Collectors.toList());
    }

    private int getRegStep(Student student) {

        if (student.getCity() == null) {
            return 1;
        }
        if (student.getUniversityId() == null) {
            return 2;
        }
        if (student.getFaculty() == null) {
            return 3;
        }
        if (student.getYear() == null) {
            return 4;
        }
        if (student.getGroupId() == null) {
            return 5;
        } else {
            return 0;
        }
    }

    public List<ManageEntity> studentBranch(String command, Student student) {

        var commandWrapper = Command.fromString(command);

        return switch (getRegStep(student)) {
            case 0 -> switch (commandWrapper.getCommand()) {
                case NOTIFICATIONS -> enableNotifications(student);
                case TIME -> setNotificationsTime(student, commandWrapper.getMsg());
                case CURRENT -> setCurrentNotifications(student);
                case ADVANCE -> setAdvanceNotifications(student);
                case SCHEDULE_TODAY -> scheduleToday(student);
                case SCHEDULE_TOMORROW -> scheduleTomorrow(student);
                case SCHEDULE_WEEK -> scheduleWeek(student);
                default -> throw new IllegalStateException("Unexpected value: " + command);
            };
            case 1 -> registerStudent2Command(command, student);
            case 2 -> registerStudent3Command(command, student);
            case 3 -> registerStudent4Command(command, student);
            case 4 -> registerStudent5Command(command, student);
            case 5 -> registerStudent6Command(command, student);
            case default -> null;
        };
    }

    public List<ManageEntity> registerStudentCommand(long chatId, List<ManageEntity> manageEntityList) {

        if (manageEntityList.isEmpty()) {
            studentRepo.save(Student.builder().chatId(chatId).build());
        }

        var bBuilder = ButtonsBuilder.builder();
        for (String city : cities) {
            bBuilder.addButton(KeyboardButton.builder()
                    .text(city).build());
        }

        manageEntityList.add(new ManageEntity(chatId, "Из какого ты города?", bBuilder.build()));
        return manageEntityList;
    }

    private List<ManageEntity> registerStudent2Command(String city, Student student) {
        var bBuilder = ButtonsBuilder.builder();
        universities.stream().filter(u -> u.getCity().equals(city)).forEach(u ->
                bBuilder.addButton(KeyboardButton.builder().text(u.getName()).build()));

        if (!cities.contains(city)) {
            return registerStudentCommand(0L,
                    new ArrayList<>(List.of(new ManageEntity(student.getChatId(), "Что-то я тебя не очень понял", null))));
        }

        student.setCity(city);
        studentRepo.save(student);

        return List.of(new ManageEntity(student.getChatId(), "Отлично, где ты учишься?", bBuilder.build()));
    }

    private List<ManageEntity> registerStudent3Command(String university, Student student) {
        long universityId = universities.stream().filter(u -> u.getName().equals(university)).findFirst().get().getId();

        List<Group> groups = groupRepo.findAllByUniversityId(universityId);

        var bBuilder = ButtonsBuilder.builder();
        groups.stream().map(Group::getFaculty).distinct().forEach(f -> bBuilder.addButton(KeyboardButton.builder()
                .text(f).build()));

        student.setUniversityId(universityId);
        studentRepo.save(student);

        return List.of(new ManageEntity(student.getChatId(), "Определим твой факультет?...Слизерин там, Грифиндор",
                bBuilder.build()));
    }

    private List<ManageEntity> registerStudent4Command(String faculty, Student student) {

        long universityId = universities.stream().filter(u -> u.getId().equals(student.getUniversityId()))
                .findFirst().get().getId();

        var bBuilder = ButtonsBuilder.builder();

        groupRepo.findAllByUniversityIdAndFaculty(universityId, faculty).stream().map(Group::getYear).distinct()
                .sorted(Integer::compareTo).forEach(year -> bBuilder.addButton(KeyboardButton.builder()
                        .text(Integer.toString(year)).build()));

        student.setFaculty(faculty);
        studentRepo.save(student);

        return List.of(new ManageEntity(student.getChatId(), "Какой курс?", bBuilder.build()));
    }

    private List<ManageEntity> registerStudent5Command(String year, Student student) {
        long universityId = universities.stream().filter(u -> u.getId().equals(student.getUniversityId()))
                .findFirst().get().getId();

        var bBuilder = ButtonsBuilder.builder();

        groupRepo.findAllByUniversityIdAndFaculty(universityId, student.getFaculty()).stream()
                .filter(g -> g.getYear() == Integer.parseInt(year)).map(Group::getName).sorted()
                .forEach(g -> bBuilder.addButton(KeyboardButton.builder().text(g).build()));

        student.setYear(Integer.parseInt(year));
        studentRepo.save(student);

        return List.of(new ManageEntity(student.getChatId(), "Какая группа?", bBuilder.build()));
    }

    private List<ManageEntity> registerStudent6Command(String group, Student student) {
        long universityId = universities.stream().filter(u -> u.getId().equals(student.getUniversityId()))
                .findFirst().get().getId();

        long groupId = groupRepo.findAllByUniversityIdAndFaculty(universityId, student.getFaculty()).stream()
                .filter(g -> g.getName().equals(group)).findFirst().get().getId();

        student.setGroupId(groupId);
        studentRepo.save(student);

        return List.of(new ManageEntity(student.getChatId(), "Отлично, я готов работе!\n" +
                        "Вот список того, что я умею:", null),
                new ManageEntity(student.getChatId(), 
                        """
                                 * /notifications - включить/выключить ежедневную рассылку расписания
                                 * /time - установить время рассылки (07:00 по умолчанию)
                                 * /current - в рассылке расписание текущего дня
                                 * /advance - в рассылке расписание следующего дня
                                 * /schedule_today - расписание на сегодня
                                 * /schedule_tomorrow - расписание на завтра
                                 * /schedule_week - расписание на неделю\
                                """, ReplyKeyboardMarkup.builder().clearKeyboard().build()));
    }

    private List<ManageEntity> scheduleToday(Student student) {
        String msg = showScheduleService.showTodaySchedule(student, null);
        return List.of(new ManageEntity(student.getChatId(), "Конечно, вот твое расписание на сегодня:", null),
                new ManageEntity(student.getChatId(), msg, null));
    }

    private List<ManageEntity> scheduleTomorrow(Student student) {
        String msg = showScheduleService.showTomorrowSchedule(student, null);
        return List.of(new ManageEntity(student.getChatId(), "Конечно, вот твое расписание на завтра:", null),
                new ManageEntity(student.getChatId(), msg, null));
    }

    private List<ManageEntity> scheduleWeek(Student student) {
        List<ManageEntity> result = new ArrayList<>(List.of(
                new ManageEntity(student.getChatId(), "Конечно, вот твое расписание на эту неделю:", null)));
        result.addAll(showScheduleService.showWeekSchedule(student, null).stream()
                .map(str -> new ManageEntity(student.getChatId(), str, null)).toList());
        return result;
    }

    private List<ManageEntity> enableNotifications(Student student) {
        return List.of(notificationsService.enableNotifications(student, null));
    }

    private List<ManageEntity> setNotificationsTime(Student student, List<String> argList) {
        var time = argList.isEmpty() ? "" : argList.get(0);

        if (time.isEmpty()) {
            return List.of(
                    new ManageEntity(student.getChatId(), """
                            Чтобы установить время рассылки, дополни команду
                            Пример:\
                            """, null),
                    new ManageEntity(student.getChatId(), "/time 08:35", null));
        } else {
            if (notificationsService.setTime(student, null, time)) {
                return List.of(new ManageEntity(student.getChatId(), "Время успешно установлено", null));
            } else {
                return List.of(new ManageEntity(student.getChatId(), "Кажется, допущена ошибка в написании времени," +
                        " попробуй еще раз", null));
            }
        }
    }

    private List<ManageEntity> setCurrentNotifications(Student student) {
        notificationsService.setIsAdvance(student, null, false);
        return List.of(new ManageEntity(student.getChatId(), "Теперь будет приходить расписание текущего дня", null));
    }

    private List<ManageEntity> setAdvanceNotifications(Student student) {
        notificationsService.setIsAdvance(student, null,true);
        return List.of(new ManageEntity(student.getChatId(), "Теперь будет приходить расписание следующего дня", null));
    }
}
