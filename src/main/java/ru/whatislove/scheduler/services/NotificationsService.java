package ru.whatislove.scheduler.services;

import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import org.springframework.stereotype.Service;
import ru.whatislove.scheduler.models.Student;
import ru.whatislove.scheduler.models.User;
import ru.whatislove.scheduler.models.UserSchedule;
import ru.whatislove.scheduler.repository.*;
import ru.whatislove.scheduler.services.telegram.ShowScheduleService;
import ru.whatislove.scheduler.services.telegram.util.ManageEntity;

@Service
public class NotificationsService {

    private final UserScheduleRepo userScheduleRepo;
    private final StudentRepo studentRepo;
    private final TeacherRepo teacherRepo;
    private final UserRepo userRepo;
    private final ShowScheduleService showScheduleService;
    private final GroupRepo groupRepo;


    public NotificationsService(UserScheduleRepo userScheduleRepo, StudentRepo studentRepo,
                                TeacherRepo teacherRepo,
                                UserRepo userRepo, ShowScheduleService showScheduleService, GroupRepo groupRepo) {
        this.userScheduleRepo = userScheduleRepo;
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
        this.userRepo = userRepo;
        this.showScheduleService = showScheduleService;
        this.groupRepo = groupRepo;
    }

    public ManageEntity enableNotifications(User user) {
        var schedule = userScheduleRepo.findAllByUserId(user.getId());

        if (!schedule.isEmpty()) {
            userScheduleRepo.deleteAllById(schedule.stream().map(UserSchedule::getId).toList());
            return new ManageEntity(user.getChatId(), "Уведомления выключены", null);
        } else {
            var userSchedule = UserSchedule.builder()
                    .userId(user.getId())
                    .notification(new Time(7, 0, 0))
                    .build();
            userScheduleRepo.save(userSchedule);
            return new ManageEntity(user.getChatId(), "Уведомления включены", null);
        }
    }

    public boolean setTime(User user, String time) {

        LocalTime parsedTime;

        try {
            parsedTime = LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (DateTimeParseException e) {
            return false;
        }

        userScheduleRepo.deleteAllByUserId(user.getId());

        var userSchedule = UserSchedule.builder()
                .userId(user.getId())
                .notification(Time.valueOf(parsedTime))
                .build();
        userScheduleRepo.save(userSchedule);
        return true;
    }

    public void setIsAdvance(User user, boolean isAdvance) {
        if (user.getRole().equals("teacher")) {
            var teacher = teacherRepo.findById(user.getRoleId()).get();
            teacher.setAdvance(isAdvance);
            teacherRepo.save(teacher);
        } else {
            var student = studentRepo.findById(user.getRoleId()).get();
            student.setAdvance(isAdvance);
            studentRepo.save(student);
        }
    }

    private void sendUserSchedule(User user, Map<Long, ManageEntity> allMessages) {
        String msg;
        boolean isAdvance;

        if (user.getRole().equals("teacher")) {
            isAdvance = teacherRepo.findById(user.getRoleId()).get().isAdvance();
        } else {
            isAdvance = studentRepo.findById(user.getRoleId()).get().isAdvance();
        }
        msg = isAdvance ? showScheduleService.showTomorrowSchedule(user) :
                showScheduleService.showTodaySchedule(user);

        allMessages.put(user.getChatId(), new ManageEntity(user.getChatId(), msg, null));
    }

    public Map<Long, ManageEntity> sendSchedule() {
        Map<Long, ManageEntity> messages = new HashMap<>();

        List<UserSchedule> userSchedules = userScheduleRepo.findAllByNotificationBetween(Time.valueOf(LocalTime.now()),
                Time.valueOf(LocalTime.now()));

        for (UserSchedule userSchedule : userSchedules) {
            sendUserSchedule(userRepo.findById(userSchedule.getUserId()).get(), messages);
        }

        return messages;
    }

    public List<ManageEntity> sendMessageToGroup(ManageEntity message, String groupStr, long universityId) {
        var group = groupRepo.findById(Long.parseLong(groupStr));

        var students = studentRepo.findAllByUniversityIdAndGroupId(universityId, group.get().getId());
        List<ManageEntity> entityList = new ArrayList<>();

        for (Student student : students) {
            message.setChatId(userRepo.findByRoleIdAndRole(student.getId(), "student").get().getChatId());
            entityList.add(message);
        }

        return entityList;
    }

    public List<ManageEntity> sendMessageToUser(ManageEntity message, long chatId) {
        message.setChatId(chatId);
        return List.of(message);
    }


}
