package ru.whatislove.scheduler.services.parser.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ru.whatislove.scheduler.config.ScheduleProperties;
import ru.whatislove.scheduler.models.StudentGroup;
import ru.whatislove.scheduler.models.Subject;
import ru.whatislove.scheduler.models.WeekParity;
import ru.whatislove.scheduler.repository.StudentGroupRepository;
import ru.whatislove.scheduler.services.parser.ScheduleParsingStrategy;

import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class LetiParser implements ScheduleParsingStrategy {

    private final ScheduleProperties properties;
    private final StudentGroupRepository studentGroupRepository;

    public LetiParser(ScheduleProperties properties, StudentGroupRepository studentGroupRepository){
        this.properties = properties;
        this.studentGroupRepository = studentGroupRepository;
    }

    @Override
    public List<Subject> parse() throws IOException {

        URL groupSetURL = new URL("https://digital.etu.ru/api/general/dicts/groups?scheduleId=publicated");

        String university = properties.getUniversities().get("Piter").get(0);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode groups = objectMapper.readTree(groupSetURL);
        List<StudentGroup> groupList = parseGroups(groups, university);

        String scheduleURL = "https://digital.etu.ru/api/schedule/objects/publicated?";
        JsonNode schedule = objectMapper.readTree(new URL(scheduleURL));

        List<Subject> result = new ArrayList<>();

        for (JsonNode group : schedule){

            StudentGroup studentGroup = groupList.stream().filter(g -> g.getGroupName()
                    .equals(group.get("fullNumber").asText())).findFirst().get();

            JsonNode scheduleObjects = group.get("scheduleObjects");

            for(JsonNode scheduleObject : scheduleObjects){

                JsonNode lesson = scheduleObject.get("lesson");

                String classroom = lesson.get("auditoriumReservation").get("auditoriumNumber").asText();

                String name = lesson.get("subject").get("title").asText() + " " +
                        lesson.get("subject").get("subjectType").asText();


                StringBuilder builder = new StringBuilder();
                Subject subject = new Subject();


                if (!lesson.get("teacher").isNull()) {

                    JsonNode teacher = lesson.get("teacher");

                    builder.append(teacher.get("surname").asText()).append(" ")
                            .append(teacher.get("name").asText()).append(" ")
                            .append(teacher.get("midname").asText()).append(" ")
                            .append(teacher.get("email").asText());

                    subject.setTeacher(builder.toString());

                }

                if (!lesson.get("auditoriumReservation").isNull()) {

                    subject.setClassroomNumber(lesson.get("auditoriumReservation").get("auditoriumNumber").asText());

                    if (!lesson.get("auditoriumReservation").get("reservationTime").isNull()){

                        subject.setStartTime(getPeriodTimeByCode(lesson.get("auditoriumReservation")
                                .get("reservationTime").get("startTime").asText()));

                        subject.setWeekDay(getWeekCode(lesson.get("auditoriumReservation")
                                .get("reservationTime").get("weekDay").asText()));

                        subject.setWeekParity(lesson.get("auditoriumReservation")
                                .get("reservationTime").get("week").asInt() == 1);
                    }
                }

                subject.setGroup(studentGroup);

                subject.setSubject(name);

                result.add(subject);
            }

        }

        return result;
    }

    private List<StudentGroup> parseGroups(JsonNode groups, String university){
        List<StudentGroup> groupList = new ArrayList<>();

        for (JsonNode group : groups) {
            String number = group.get("fullNumber").asText();

            StudentGroup studentGroup = new StudentGroup(number, university);
            groupList.add(studentGroup);
        }

        studentGroupRepository.saveAll(groupList);
        return studentGroupRepository.findAllByUniversity(university);
    }


    private short getWeekCode(String code){

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

    private LocalTime getPeriodTimeByCode(String code){

        return switch (code) {
            case "100" -> LocalTime.of(8, 0);
            case "101" -> LocalTime.of(9, 50);
            case "102" -> LocalTime.of(11, 40);
            case "103" -> LocalTime.of(13, 40);
            case "104" -> LocalTime.of(15, 30);
            case "105" -> LocalTime.of(17, 20);
            case "106" -> LocalTime.of(19, 05);
            default -> LocalTime.of(20, 50);
        };
    }
}
