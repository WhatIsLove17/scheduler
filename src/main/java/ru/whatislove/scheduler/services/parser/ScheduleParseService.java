package ru.whatislove.scheduler.services.parser;


import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.whatislove.scheduler.config.ScheduleProperties;
import ru.whatislove.scheduler.models.Subject;
import ru.whatislove.scheduler.repository.StudentGroupRepository;
import ru.whatislove.scheduler.repository.SubjectRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleParseService {

    private final ScheduleParser scheduleParser;

    private final ListableBeanFactory beanFactory;
    private final ScheduleProperties properties;
    private final List<ScheduleParsingStrategy> parsers;

    private final StudentGroupRepository studentGroupRepository;

    private final SubjectRepository subjectRepository;


    @Autowired
    public ScheduleParseService(ListableBeanFactory beanFactory, ScheduleParser scheduleParser,
                                ScheduleProperties properties, StudentGroupRepository studentGroupRepository,
                                SubjectRepository subjectRepository){
        this.properties = properties;
        this.beanFactory = beanFactory;
        this.scheduleParser = scheduleParser;

        parsers = new ArrayList<>(beanFactory.getBeansOfType(ScheduleParsingStrategy.class).values());
        this.studentGroupRepository = studentGroupRepository;
        this.subjectRepository = subjectRepository;

        parseSchedule();
    }


    @Scheduled(cron = "0 0 1 * * ?")
    public void parseSchedule() {

        studentGroupRepository.deleteAll();
        subjectRepository.deleteAll();

        for(ScheduleParsingStrategy parser : parsers){

            scheduleParser.setStrategy(parser);

            try {
                List<Subject> subjects = scheduleParser.parseSchedule();
                subjectRepository.saveAll(subjects);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }



    }
}
