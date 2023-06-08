package ru.whatislove.scheduler.repository;

import org.springframework.data.repository.CrudRepository;
import ru.whatislove.scheduler.models.Discipline;
import ru.whatislove.scheduler.models.File;

import java.util.List;

public interface FileRepo extends CrudRepository<File, Long> {

    List<File> findAllByReceiverIdAndReceiverType(long id, String type);
}
