package ru.whatislove.scheduler.services.parser;

import ru.whatislove.scheduler.models.Subject;

import java.io.IOException;
import java.util.List;

public interface ScheduleParsingStrategy {
    List<Subject> parse() throws IOException;
}
