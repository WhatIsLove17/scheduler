package ru.whatislove.scheduler.repository.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.whatislove.scheduler.models.Subject;
import ru.whatislove.scheduler.models.WeekParity;
import ru.whatislove.scheduler.repository.SubjectRepository;

import java.sql.Time;
import java.util.List;


@Service
public class SubjectRepositoryImpl implements SubjectRepository {

    private final JdbcTemplate jdbcTemplate;

    public SubjectRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Subject> findAll() {
        return null;
    }

    @Override
    public Subject findById(Long id) {
        return null;
    }

    @Override
    public void saveAll(List<Subject> subjectList) {

        System.out.println(WeekParity.FIRST.name());

        String sql = "INSERT INTO subjects (start_time, week_day, subject, teacher, group_id, classroom_number," +
                " week_parity) VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, subjectList, subjectList.size(),
                (ps, subject) -> {
                    ps.setTime(1, Time.valueOf(subject.getStartTime()));
                    ps.setInt(2, subject.getWeekDay());
                    ps.setString(3, subject.getSubject());
                    ps.setString(4, subject.getTeacher());
                    ps.setLong(5, subject.getGroup().getId());
                    ps.setString(6, subject.getClassroomNumber());
                    ps.setBoolean(7, subject.getWeekParity());
                });

        System.out.println("Done");
    }


    @Override
    public void update(Subject subject) {

    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM subjects");
    }
}
