package ru.whatislove.scheduler.repository;

import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import ru.whatislove.scheduler.models.UserSchedule;

public interface UserScheduleRepo extends CrudRepository<UserSchedule, Long> {

    void deleteAllByUserId(long userId);

    List<UserSchedule> findAllByUserId(long userId);

    List<UserSchedule> findAllByNotificationBetween(Time notification, Time notification2);

}
