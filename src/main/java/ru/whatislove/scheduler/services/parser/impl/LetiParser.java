package ru.whatislove.scheduler.services.parser.impl;

import java.io.IOException;
import java.net.URL;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ru.whatislove.scheduler.models.Discipline;
import ru.whatislove.scheduler.models.Group;
import ru.whatislove.scheduler.models.Teacher;
import ru.whatislove.scheduler.models.University;
import ru.whatislove.scheduler.repository.GroupRepo;
import ru.whatislove.scheduler.repository.TeacherRepo;
import ru.whatislove.scheduler.repository.UniversityRepo;
import ru.whatislove.scheduler.services.parser.ScheduleParsingStrategy;

@Component
public class LetiParser implements ScheduleParsingStrategy {

    private final University university;
    private final GroupRepo groupRepo;
    private final TeacherRepo teacherRepo;
    private final UniversityRepo universityRepo;

    public LetiParser(UniversityRepo universityRepo, GroupRepo groupRepo, TeacherRepo teacherRepo) {
        university = universityRepo.findById(1L).get();
        this.universityRepo = universityRepo;
        this.groupRepo = groupRepo;
        this.teacherRepo = teacherRepo;

        DayOfWeek weekDay = LocalDate.now().getDayOfWeek();
        if (weekDay.equals(DayOfWeek.MONDAY)) {
            university.setWeekParity(university.getWeekParity() == 1 ? 2 : 1);
            universityRepo.save(university);
        }
    }

    @Override
    public List<Discipline> parse() throws IOException {

        URL groupSetURL = new URL("https://digital.etu.ru/api/general/dicts/groups?scheduleId=publicated" +
                "&withFaculty=true");


        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode groups = objectMapper.readTree(groupSetURL);
        List<Group> groupList = parseGroups(groups);

        String scheduleURL = "https://digital.etu.ru/api/schedule/objects/publicated?";
        JsonNode schedule = objectMapper.readTree(new URL(scheduleURL));

        List<Discipline> result = new ArrayList<>();

        for (JsonNode group : schedule) {

            Group studentGroup = groupList.stream().filter(g -> g.getName()
                    .equals(group.get("fullNumber").asText())).findFirst().get();

            JsonNode scheduleObjects = group.get("scheduleObjects");

            for (JsonNode scheduleObject : scheduleObjects) {

                JsonNode lesson = scheduleObject.get("lesson");

                String classroom = lesson.get("auditoriumReservation").get("auditoriumNumber").asText();

                String name = lesson.get("subject").get("title").asText() + " " +
                        lesson.get("subject").get("subjectType").asText();

                StringBuilder builder = new StringBuilder();
                Discipline subject = new Discipline();

                if (!lesson.get("teacher").isNull()) {

                    JsonNode teacher = lesson.get("teacher");

                    builder.append(teacher.get("surname").asText()).append(" ")
                            .append(teacher.get("name").asText()).append(" ")
                            .append(teacher.get("midname").asText());
                    Teacher teacherForSave = new Teacher(null, builder.toString(),
                            teacher.get("email").asText(), university.getId(), false,
                            UUID.randomUUID().toString());

                    Optional<Teacher> teacherFromRepo = teacherRepo.findAllByUniversityIdAndName(university.getId(),
                            builder.toString());

                    if (teacherFromRepo.isEmpty()) {
                        teacherRepo.save(teacherForSave);
                    }
                    long teacherId = teacherRepo.findAllByUniversityIdAndName(university.getId(),
                            builder.toString()).get().getId();

                    subject.setTeacherId(teacherId);
                }

                if (!lesson.get("auditoriumReservation").isNull()) {

                    subject.setAuditory(lesson.get("auditoriumReservation").get("auditoriumNumber").asText());

                    if (!lesson.get("auditoriumReservation").get("reservationTime").isNull()) {

                        subject.setTime(getPeriodTimeByCode(lesson.get("auditoriumReservation")
                                .get("reservationTime").get("startTime").asText()));

                        subject.setWeekDay(getWeekCode(lesson.get("auditoriumReservation")
                                .get("reservationTime").get("weekDay").asText()));

                        subject.setWeekParity(lesson.get("auditoriumReservation")
                                .get("reservationTime").get("week").asInt());
                    }
                }

                subject.setGroupId(studentGroup.getId());

                subject.setName(name);

                result.add(subject);
            }

        }

        return result;
    }

    private List<Group> parseGroups(JsonNode groups) {
        List<Group> groupList = new ArrayList<>();

        for (JsonNode group : groups) {
            String number = group.get("fullNumber").asText();
            String faculty = group.get("department").get("faculty").get("title").asText();
            int course = group.get("course").asInt();

            Group studentGroup = new Group(null, number, university.getId(), faculty, course);
            groupList.add(studentGroup);
        }

        groupRepo.saveAll(groupList);
        return groupList;
    }


    private short getWeekCode(String code) {

        return switch (code) {
            case "MON" -> 1;
            case "TUE" -> 2;
            case "WED" -> 3;
            case "THU" -> 4;
            case "FRI" -> 5;
            case "SAT" -> 6;
            default -> 7;
        };
    }

    private Time getPeriodTimeByCode(String code) {

        return Time.valueOf(switch (code) {
            case "100" -> LocalTime.of(8, 0);
            case "101" -> LocalTime.of(9, 50);
            case "102" -> LocalTime.of(11, 40);
            case "103" -> LocalTime.of(13, 40);
            case "104" -> LocalTime.of(15, 30);
            case "105" -> LocalTime.of(17, 20);
            case "106" -> LocalTime.of(19, 5);
            default -> LocalTime.of(20, 50);
        });
    }
}
