package ru.whatislove.scheduler.repository.impl;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.whatislove.scheduler.models.StudentGroup;
import ru.whatislove.scheduler.repository.StudentGroupRepository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Service
public class StudentGroupRepositoryImpl implements StudentGroupRepository {

    private final JdbcTemplate jdbcTemplate;

    public StudentGroupRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public List<StudentGroup> findAllByUniversity(String university) {
        return jdbcTemplate.query("SELECT * FROM student_groups WHERE university = ?",
                new Object[]{university}, new BeanPropertyRowMapper<>(StudentGroup.class));
    }

    @Override
    public StudentGroup findById(Long id) {
        return null;
    }

    @Override
    public void saveAll(List<StudentGroup> groups) {
        jdbcTemplate.batchUpdate("INSERT INTO student_groups(group_name, university) VALUES(?, ?)",
                new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, groups.get(i).getGroupName());
                ps.setString(2, groups.get(i).getUniversity());
            }

            @Override
            public int getBatchSize() {
                return groups.size();
            }
        });
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM student_groups");
    }
}