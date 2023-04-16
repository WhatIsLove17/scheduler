package ru.whatislove.scheduler.models;


import lombok.Data;

@Data
public class Student {
    private Long id;
    private String name;
    private Long chatId;
    private StudentGroup group;

}
