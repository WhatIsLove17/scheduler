package ru.whatislove.scheduler.services.telegram.util;

import java.util.ArrayList;
import java.util.Arrays;

public enum UserCommand {

    TEACHER_REG("/teacherReg"),
    STUDENT_REG("/studentReg"),
    NON_COMMAND("fail");

    private final String userCommand;

    UserCommand(String userCommand) {
        this.userCommand = userCommand;
    }

    public String getUserCommand() {
        return userCommand;
    }

    public static CommandWrapper<UserCommand> fromString(String command) {
        for (UserCommand c : UserCommand.values()) {
            if (command.contains(c.getUserCommand())) {
                var list = new ArrayList<>(Arrays.stream(command.split(" ")).toList());
                list.remove(0);
                return new CommandWrapper<UserCommand>(c, list);
            }
        }
        return new CommandWrapper<>(NON_COMMAND, null);
    }
}
