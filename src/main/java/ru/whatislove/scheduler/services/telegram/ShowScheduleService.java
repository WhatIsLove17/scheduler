package ru.whatislove.scheduler.services.telegram;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.compress.utils.Lists;
import org.springframework.stereotype.Service;
import ru.whatislove.scheduler.models.Discipline;
import ru.whatislove.scheduler.models.Group;
import ru.whatislove.scheduler.models.Student;
import ru.whatislove.scheduler.models.Teacher;
import ru.whatislove.scheduler.repository.DisciplineRepo;
import ru.whatislove.scheduler.repository.GroupRepo;
import ru.whatislove.scheduler.repository.TeacherRepo;
import ru.whatislove.scheduler.repository.UniversityRepo;

@Service
public class ShowScheduleService {

    private final DisciplineRepo disciplineRepo;

    private final UniversityRepo universityRepo;

    private final TeacherRepo teacherRepo;

    private final GroupRepo groupRepo;

    public ShowScheduleService(DisciplineRepo disciplineRepo, UniversityRepo universityRepo, TeacherRepo teacherRepo,
                               GroupRepo groupRepo) {
        this.disciplineRepo = disciplineRepo;
        this.universityRepo = universityRepo;
        this.teacherRepo = teacherRepo;
        this.groupRepo = groupRepo;
    }

    public String showTodaySchedule(Student student, Teacher teacher) {
        DayOfWeek weekDay = LocalDate.now().getDayOfWeek();

        int weekParity = universityRepo.findById(student != null ? student.getUniversityId()
                : teacher.getUniversityId()).get().getWeekParity();

        return showScheduleForDay(student, teacher, weekDay, weekParity);
    }

    public String showTomorrowSchedule(Student student, Teacher teacher) {
        DayOfWeek weekDay = LocalDate.now().getDayOfWeek().plus(1);

        int weekParity = universityRepo.findById(student != null ? student.getUniversityId()
                : teacher.getUniversityId()).get().getWeekParity();

        if (weekDay.equals(DayOfWeek.SUNDAY)) {
            weekParity = weekParity == 1 ? 2 : 1;
        }

        return showScheduleForDay(student, teacher, weekDay, weekParity);
    }

    public List<String> showWeekSchedule(Student student, Teacher teacher) {
        int weekParity = universityRepo.findById(student != null ? student.getUniversityId()
                : teacher.getUniversityId()).get().getWeekParity();

        List<String> days = new ArrayList<>();
        for (DayOfWeek weekDay : DayOfWeek.values()) {
            days.add(showScheduleForDay(student, teacher, weekDay, weekParity));
        }

        return days;
    }

    public String showScheduleForDay(Student student, Teacher teacher, DayOfWeek weekDay, int weekParity) {

        List<Discipline> disciplineList;

        if (student != null) {
            disciplineList = disciplineRepo.findAllByGroupIdAndWeekDay(student.getGroupId(),
                    weekDay.getValue());
        } else {
            disciplineList = disciplineRepo.findAllByTeacherIdAndWeekDay(teacher.getId(),
                    weekDay.getValue());
        }

        List<Teacher> teachers = new ArrayList<>();
        List<Group> groups;
        if (student != null) {
            teachers = Lists.newArrayList(teacherRepo.findAllById(disciplineList.stream()
                    .map(Discipline::getTeacherId).toList()).iterator());
        }
        else {
            groups = Lists.newArrayList(groupRepo.findAllById(disciplineList.stream()
                    .map(Discipline::getGroupId).toList()).iterator());
        }
        StringBuilder builder = new StringBuilder(weekDay.toString()).append(":\n");

        disciplineList.stream().sorted(Comparator.comparing(Discipline::getTime)).forEach(
                discipline -> {
                    if (discipline.getWeekParity() == weekParity) {
                        builder.append(" * ").append(discipline.getTime())
                                .append(' ').append(discipline.getName()).append(' ').append(discipline.getAuditory())
                                .append(" (").append(student != null ? teachers.stream().filter(t -> Objects.equals(t.getId(),
                                                discipline.getTeacherId()))
                                        .map(Teacher::getName).findFirst().orElse("") :
                                        groups.stream().filter(g -> g.getId().equals(discipline.getGroupId()))
                                                .map(Group::getName).findFirst().orElse(""))
                                .append(") ").append('\n');
                    }
                }
        );

        return builder.toString();
    }
}
