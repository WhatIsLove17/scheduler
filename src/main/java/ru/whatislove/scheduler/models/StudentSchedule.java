package ru.whatislove.scheduler.models;


import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class StudentSchedule {
    private Long id;
    private Student student;
    private DayOfWeek dayOfWeek;
    private LocalTime sendTime;
}
