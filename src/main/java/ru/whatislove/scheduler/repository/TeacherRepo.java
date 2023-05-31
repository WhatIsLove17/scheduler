package ru.whatislove.scheduler.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import ru.whatislove.scheduler.models.Teacher;

public interface TeacherRepo extends CrudRepository<Teacher, Long> {

    List<Teacher> findAllByUniversityId(long id);

    Optional<Teacher> findAllByUniversityIdAndName(long universityId, String name);

    Optional<Teacher> findTeacherByChatId(long chatId);

    void deleteTeacherByChatId(long chatId);

    Optional<Teacher> findTeacherByKey(String key);
}
