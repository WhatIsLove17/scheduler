package ru.whatislove.scheduler.models;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "disciplines")
public class Discipline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private long groupId;
    private int weekDay;
    private int weekParity;
    private Time time;
    private Long teacherId;
    private String auditory;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Discipline that = (Discipline) o;
        return weekDay == that.weekDay && weekParity == that.weekParity && Objects.equals(name, that.name) && Objects.equals(time, that.time) && Objects.equals(teacherId, that.teacherId) && Objects.equals(auditory, that.auditory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, weekDay, weekParity, time, teacherId, auditory);
    }
}


