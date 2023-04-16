package ru.whatislove.scheduler.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "schedule")
@Component
@Data
public class ScheduleProperties {

    private Map<String, List<String>> universities;
}

