package ru.whatislove.scheduler.services.telegram.util;

import java.util.ArrayList;
import java.util.Arrays;

public enum Command {

    NOTIFICATIONS("/notifications"),
    TIME("/time"),
    CURRENT("/current"),
    ADVANCE("/advance"),
    SCHEDULE_TODAY("/schedule_today"),
    SCHEDULE_TOMORROW("/schedule_tomorrow"),
    SCHEDULE_WEEK("/schedule_week"),
    SEND_MESSAGE("/send_message"),
    NON_COMMAND("fail");

    private final String command;

    Command(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public static CommandWrapper fromString(String command) {
        for (Command c : Command.values()) {
            if (command.contains(c.getCommand())) {
                var list = new ArrayList<>(Arrays.stream(command.split(" ")).toList());
                list.remove(0);
                return new CommandWrapper(c, list);
            }
        }
        return new CommandWrapper(NON_COMMAND, null);
    }
}
