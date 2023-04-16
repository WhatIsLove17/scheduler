package ru.whatislove.scheduler.models;


import lombok.Data;

import java.time.LocalTime;

@Data
public class Subject {
    private Long id;
    private LocalTime startTime;
    private short weekDay;
    private String subject;
    private String teacher;
    private StudentGroup group;
    private String classroomNumber;
    private boolean weekParity;
}


