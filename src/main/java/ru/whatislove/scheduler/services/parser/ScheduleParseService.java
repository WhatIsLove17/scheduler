package ru.whatislove.scheduler.services.parser;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.whatislove.scheduler.models.Discipline;
import ru.whatislove.scheduler.repository.DisciplineRepo;
import ru.whatislove.scheduler.repository.GroupRepo;

@Service
public class ScheduleParseService {

    private final ScheduleParser scheduleParser;

    private final List<ScheduleParsingStrategy> parsers;

    private final GroupRepo groupRepo;

    private final DisciplineRepo disciplineRepo;

    public ScheduleParseService(ListableBeanFactory beanFactory, ScheduleParser scheduleParser, GroupRepo groupRepo,
                                DisciplineRepo disciplineRepo) {
        this.scheduleParser = scheduleParser;

        parsers = new ArrayList<>(beanFactory.getBeansOfType(ScheduleParsingStrategy.class).values());
        this.groupRepo = groupRepo;
        this.disciplineRepo = disciplineRepo;

        //parseSchedule();
    }


    @Scheduled(cron = "0 0 0 * * ?")
    public void parseSchedule() {

        disciplineRepo.deleteAll();

        for (ScheduleParsingStrategy parser : parsers) {

            scheduleParser.setStrategy(parser);

            try {
                List<Discipline> subjects = scheduleParser.parseSchedule();
                disciplineRepo.saveAll(subjects);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }
}
