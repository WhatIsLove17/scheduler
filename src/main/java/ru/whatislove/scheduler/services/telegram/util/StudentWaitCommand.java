package ru.whatislove.scheduler.services.telegram.util;

import java.util.ArrayList;
import java.util.Arrays;

public enum StudentWaitCommand {

    WAIT_CITY("/waitCity"),
    WAIT_UNIVERSITY("/waitUniversity"),
    WAIT_FACULTY("/waitFaculty"),
    WAIT_YEAR("/waitYear"),
    WAIT_GROUP("/waitGroup"),
    WAIT_TIME("/waitTime"),
    WAIT_NAME("/waitName"),
    WAIT_TEACHER_ID("/waitTeacherId"),
    WAIT_TEXT_USER("/waitTextUser"),
    WAIT_RESOURCE("/waitSource");

    private final String userCommand;

    StudentWaitCommand(String userCommand) {
        this.userCommand = userCommand;
    }

    public String getUserCommand() {
        return userCommand;
    }

    public static CommandWrapper<StudentWaitCommand> fromString(String command) {
        for (StudentWaitCommand c : StudentWaitCommand.values()) {
            if (command.contains(c.getUserCommand())) {
                var list = new ArrayList<>(Arrays.stream(command.split(" ")).toList());
                list.remove(0);
                return new CommandWrapper<>(c, list);
            }
        }
        return null;
    }
}
