package ru.whatislove.scheduler.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import ru.whatislove.scheduler.models.Group;

public interface GroupRepo extends CrudRepository<Group, Long> {

    List<Group> findAllByUniversityId(long id);

    List<Group> findAllByUniversityIdAndFaculty(long id, String faculty);

    Optional<Group> findByUniversityIdAndName(long id, String name);
}
