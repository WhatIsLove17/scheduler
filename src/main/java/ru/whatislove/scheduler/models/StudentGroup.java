package ru.whatislove.scheduler.models;

import lombok.Data;

@Data
public class StudentGroup {

    private Long id;
    private String groupName;

    private String university;

    public StudentGroup(String groupName, String university){
        this.groupName = groupName;
        this.university = university;
    }

    public StudentGroup(){}
}
