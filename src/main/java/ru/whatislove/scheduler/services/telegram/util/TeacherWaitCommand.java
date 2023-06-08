package ru.whatislove.scheduler.services.telegram.util;

import java.util.ArrayList;
import java.util.Arrays;

public enum TeacherWaitCommand {

    WAIT_KEY("/waitKey"),
    WAIT_TIME("/waitTime"),
    WAIT_GROUP_ID("/waitGroupId"),
    WAIT_USER_ID("/waitUserId"),
    WAIT_TEXT_GROUP("/waitTextGroup"),
    WAIT_TEXT_USER("/waitTextUser");

    private final String userCommand;

    TeacherWaitCommand(String userCommand) {
        this.userCommand = userCommand;
    }

    public String getUserCommand() {
        return userCommand;
    }

    public static CommandWrapper<TeacherWaitCommand> fromString(String command) {
        for (TeacherWaitCommand c : TeacherWaitCommand.values()) {
            if (command.contains(c.getUserCommand())) {
                var list = new ArrayList<>(Arrays.stream(command.split(" ")).toList());
                list.remove(0);
                return new CommandWrapper<TeacherWaitCommand>(c, list);
            }
        }
        return null;
    }
}
