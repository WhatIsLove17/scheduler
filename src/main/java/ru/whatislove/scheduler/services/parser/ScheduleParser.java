package ru.whatislove.scheduler.services.parser;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import ru.whatislove.scheduler.models.Discipline;

@Service
public class ScheduleParser {
    private ScheduleParsingStrategy strategy;

    public void setStrategy(ScheduleParsingStrategy strategy) {
        this.strategy = strategy;
    }

    public List<Discipline> parseSchedule() throws IOException {
        return strategy.parse();
    }
}
