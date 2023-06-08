package ru.whatislove.scheduler.repository;

import org.springframework.data.repository.CrudRepository;
import ru.whatislove.scheduler.models.User;

import java.util.Optional;

public interface UserRepo extends CrudRepository<User, Long> {
    Optional<User> findByChatId(long chatId);

    Optional<User> findByRoleIdAndRole(long roleId, String role);

}
