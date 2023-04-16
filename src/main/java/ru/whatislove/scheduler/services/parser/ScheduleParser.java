package ru.whatislove.scheduler.services.parser;

import org.springframework.stereotype.Service;
import ru.whatislove.scheduler.models.Subject;

import java.io.IOException;
import java.util.List;

@Service
public class ScheduleParser {
    private ScheduleParsingStrategy strategy;

    public void setStrategy(ScheduleParsingStrategy strategy) {
        this.strategy = strategy;
    }

    public List<Subject> parseSchedule() throws IOException {
        return strategy.parse();
    }
}
