package ru.whatislove.scheduler.services.parser;


import java.io.IOException;
import java.util.List;

import ru.whatislove.scheduler.models.Discipline;

public interface ScheduleParsingStrategy {
    List<Discipline> parse() throws IOException;
}
