package ru.whatislove.scheduler.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import ru.whatislove.scheduler.models.Discipline;

public interface DisciplineRepo extends CrudRepository<Discipline, Long> {

    List<Discipline> findAllByGroupIdAndWeekDay(long groupId, int weekDay);

    List<Discipline> findAllByTeacherIdAndWeekDay(long teacherId, int weekDay);
}
