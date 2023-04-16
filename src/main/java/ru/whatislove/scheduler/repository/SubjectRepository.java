package ru.whatislove.scheduler.repository;

import ru.whatislove.scheduler.models.Subject;

import java.util.List;

public interface SubjectRepository {
    public List<Subject> findAll();
    public Subject findById(Long id);
    public void saveAll(List<Subject> subjectList);
    public void update(Subject subject);
    public void deleteAll();
}
