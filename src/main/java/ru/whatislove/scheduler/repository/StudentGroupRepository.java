package ru.whatislove.scheduler.repository;

import ru.whatislove.scheduler.models.StudentGroup;

import java.util.List;

public interface StudentGroupRepository {
    public List<StudentGroup> findAllByUniversity(String university);
    public StudentGroup findById(Long id);
    public void saveAll(List<StudentGroup> groups);

    public void deleteAll();
}
