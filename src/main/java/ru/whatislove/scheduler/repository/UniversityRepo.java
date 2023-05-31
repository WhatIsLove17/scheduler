package ru.whatislove.scheduler.repository;

import org.springframework.data.repository.CrudRepository;
import ru.whatislove.scheduler.models.University;

public interface UniversityRepo extends CrudRepository<University, Long> {
}
