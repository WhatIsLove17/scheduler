package ru.whatislove.scheduler.services.telegram;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.compress.utils.Lists;
import org.springframework.stereotype.Service;
import ru.whatislove.scheduler.models.*;
import ru.whatislove.scheduler.repository.*;

@Service
public class ShowScheduleService {

    private final DisciplineRepo disciplineRepo;

    private final UniversityRepo universityRepo;

    private final TeacherRepo teacherRepo;
    private final StudentRepo studentRepo;
    private final GroupRepo groupRepo;

    public ShowScheduleService(DisciplineRepo disciplineRepo, UniversityRepo universityRepo, TeacherRepo teacherRepo,
                               StudentRepo studentRepo, GroupRepo groupRepo) {
        this.disciplineRepo = disciplineRepo;
        this.universityRepo = universityRepo;
        this.teacherRepo = teacherRepo;
        this.studentRepo = studentRepo;
        this.groupRepo = groupRepo;
    }

    public String showTodaySchedule(User user) {
        DayOfWeek weekDay = LocalDate.now().getDayOfWeek();

        int weekParity = getWeekPArity(user);

        return showScheduleForDay(user, weekDay, weekParity);
    }

    public String showTomorrowSchedule(User user) {
        DayOfWeek weekDay = LocalDate.now().getDayOfWeek().plus(1);

        int weekParity = getWeekPArity(user);

        if (weekDay.equals(DayOfWeek.SUNDAY)) {
            weekParity = weekParity == 1 ? 2 : 1;
        }

        return showScheduleForDay(user, weekDay, weekParity);
    }

    public List<String> showWeekSchedule(User user) {
        int weekParity = getWeekPArity(user);


        List<String> days = new ArrayList<>();
        for (DayOfWeek weekDay : DayOfWeek.values()) {
            days.add(showScheduleForDay(user, weekDay, weekParity));
        }

        return days;
    }

    public String showScheduleForDay(User user, DayOfWeek weekDay, int weekParity) {
        if (user.getRole().equals("teacher")) {
            var teacher = teacherRepo.findById(user.getRoleId()).get();
            return showTeacherScheduleForDay(teacher, weekDay, weekParity);
        }
        else {
            var student = studentRepo.findById(user.getRoleId()).get();
            return showStudentScheduleForDay(student, weekDay, weekParity);
        }
    }

    public String showStudentScheduleForDay(Student student, DayOfWeek weekDay, int weekParity) {

        List<Discipline> disciplineList;

        disciplineList = disciplineRepo.findAllByGroupIdAndWeekDay(student.getGroupId(),
                weekDay.getValue());

        final List<Teacher> teachers;
        teachers = Lists.newArrayList(teacherRepo.findAllById(disciplineList.stream()
                .map(Discipline::getTeacherId).toList()).iterator());
        StringBuilder builder = new StringBuilder("\uD83D\uDCC5 " + weekDay).append(":\n\n");

        disciplineList.stream().sorted(Comparator.comparing(Discipline::getTime)).forEach(
                discipline -> {
                    if (discipline.getWeekParity() == weekParity) {
                        builder.append(" ⏰ ").append(discipline.getTime())
                                .append(' ').append(discipline.getName()).append("\n \uD83D\uDCDA ").append(discipline.getAuditory())
                                .append("\n \uD83C\uDF93 ").append(teachers.stream().filter(t -> Objects.equals(t.getId(),
                                        discipline.getTeacherId())).map(Teacher::getName).findFirst().orElse(""))
                                .append("\n\n");
                    }
                }
        );

        return builder.toString();
    }

    public String showTeacherScheduleForDay(Teacher teacher, DayOfWeek weekDay, int weekParity) {

        List<Discipline> disciplineList;

        disciplineList = disciplineRepo.findAllByTeacherIdAndWeekDay(teacher.getId(),
                weekDay.getValue());

        final List<Group> groups;
        groups = Lists.newArrayList(groupRepo.findAllById(disciplineList.stream()
                .map(Discipline::getGroupId).toList()).iterator());
        StringBuilder builder = new StringBuilder(weekDay.toString()).append(":\n");

        disciplineList.stream().sorted(Comparator.comparing(Discipline::getTime)).forEach(
                discipline -> {
                    if (discipline.getWeekParity() == weekParity) {
                        builder.append(" * ").append(discipline.getTime())
                                .append(' ').append(discipline.getName()).append(' ').append(discipline.getAuditory())
                                .append(" (").append(groups.stream().filter(g -> g.getId().equals(discipline.getGroupId()))
                                        .map(Group::getName).findFirst().orElse(""))
                                .append(") ").append('\n');
                    }
                }
        );

        return builder.toString();
    }

    public int getWeekPArity(User user) {
        return universityRepo.findById(user.getRole().equals("teacher") ?
                teacherRepo.findById(user.getRoleId()).get().getUniversityId() :
                studentRepo.findById(user.getRoleId()).get().getUniversityId()).get().getWeekParity();
    }
}
