package ru.whatislove.scheduler.services;

import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import ru.whatislove.scheduler.models.Student;
import ru.whatislove.scheduler.models.Teacher;
import ru.whatislove.scheduler.models.UserSchedule;
import ru.whatislove.scheduler.repository.GroupRepo;
import ru.whatislove.scheduler.repository.StudentRepo;
import ru.whatislove.scheduler.repository.TeacherRepo;
import ru.whatislove.scheduler.repository.UserScheduleRepo;
import ru.whatislove.scheduler.services.telegram.ShowScheduleService;
import ru.whatislove.scheduler.services.telegram.util.ManageEntity;

@Service
public class NotificationsService {

    private final UserScheduleRepo userScheduleRepo;
    private final StudentRepo studentRepo;
    private final TeacherRepo teacherRepo;
    private final ShowScheduleService showScheduleService;
    private final GroupRepo groupRepo;


    public NotificationsService(UserScheduleRepo userScheduleRepo, StudentRepo studentRepo,
                                TeacherRepo teacherRepo,
                                ShowScheduleService showScheduleService, GroupRepo groupRepo) {
        this.userScheduleRepo = userScheduleRepo;
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
        this.showScheduleService = showScheduleService;
        this.groupRepo = groupRepo;
    }

    public ManageEntity enableNotifications(Student student, Teacher teacher) {
        Optional<UserSchedule> schedule;
        if (student != null) {
            schedule = userScheduleRepo.findByStudentId(student.getId());
        } else {
            schedule = userScheduleRepo.findByTeacherId(teacher.getId());
        }
        long chatId = student != null ? student.getChatId() : teacher.getChatId();

        if (schedule.isPresent()) {
            userScheduleRepo.delete(schedule.get());
            return new ManageEntity(chatId,"Уведомления выключены", null);
        } else {
            var userSchedule = UserSchedule.builder()
                    .studentId(student.getId())
                    .notification(new Time(7, 0, 0))
                    .build();
            userScheduleRepo.save(userSchedule);
            return new ManageEntity(chatId,"Уведомления включены", null);
        }
    }

    public boolean setTime(Student student, Teacher teacher, String time) {

        LocalTime parsedTime;

        try {
            parsedTime = LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (DateTimeParseException e) {
            return false;
        }

        if (student != null) {
            userScheduleRepo.deleteAllByStudentId(student.getId());
        } else {
            userScheduleRepo.deleteAllByTeacherId(teacher.getId());
        }

        var userSchedule = UserSchedule.builder()
                .studentId(student != null ? student.getId() : null)
                .teacherId(teacher != null ? teacher.getId() : null)
                .notification(Time.valueOf(parsedTime))
                .build();
        userScheduleRepo.save(userSchedule);
        return true;
    }

    public void setIsAdvance(Student student, Teacher teacher, boolean isAdvance) {
        if (student != null) {
            student.setAdvance(isAdvance);
            studentRepo.save(student);
        } else {
            teacher.setAdvance(isAdvance);
            teacherRepo.save(teacher);
        }
    }

    private void sendUserSchedule(Student student, Teacher teacher, Map<Long, ManageEntity> allMessages) {
        String msg;

        long chatId;

        if (student != null) {
            msg = student.isAdvance() ? showScheduleService.showTomorrowSchedule(student, teacher) :
                    showScheduleService.showTodaySchedule(student, teacher);
            chatId = student.getChatId();
        } else {
            msg = teacher.isAdvance() ? showScheduleService.showTomorrowSchedule(student, teacher) :
                    showScheduleService.showTodaySchedule(student, teacher);
            chatId = teacher.getChatId();
        }

        allMessages.put(chatId, new ManageEntity(chatId, msg, null));
    }

    public Map<Long, ManageEntity> sendSchedule() {
        Map<Long, ManageEntity> messages = new HashMap<>();

        List<UserSchedule> userSchedules = userScheduleRepo.findAllByNotificationBetween(Time.valueOf(LocalTime.now()),
                Time.valueOf(LocalTime.now()));

        for (UserSchedule userSchedule : userSchedules) {
            if (userSchedule.getStudentId() != null) {
                sendUserSchedule(studentRepo.findById(userSchedule.getStudentId()).get(), null, messages);
            } else {
                sendUserSchedule(null, teacherRepo.findById(userSchedule.getTeacherId()).get(), messages);
            }
        }

        return messages;
    }

    public List<ManageEntity> sendMessage(String message, String groupStr, long universityId) {
        var group = groupRepo.findByUniversityIdAndName(universityId, groupStr);

        var students = studentRepo.findAllByUniversityIdAndGroupId(universityId, group.get().getId());
        List<ManageEntity> entityList = new ArrayList<>();

        for(Student student : students) {
            entityList.add(new ManageEntity(student.getChatId(), message, null));
        }

        return  entityList;
    }



}
