package ru.whatislove.scheduler.services.telegram.util;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommandWrapper <T> {
    private T command;
    private List<String> msg;
}
