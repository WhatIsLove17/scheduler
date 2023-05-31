package ru.whatislove.scheduler.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import ru.whatislove.scheduler.models.Student;

public interface StudentRepo extends CrudRepository<Student, Long> {

    Optional<Student> findStudentByChatId(long chatId);

    void deleteStudentByChatId(long chatId);

    List<Student> findAllByUniversityIdAndGroupId(long universityId, long groupId);
}
