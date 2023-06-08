package ru.whatislove.scheduler.services.telegram.util;

import org.springframework.stereotype.Service;
import ru.whatislove.scheduler.models.File;
import ru.whatislove.scheduler.repository.FileRepo;

@Service
public class FileSaver {
    private final FileRepo fileRepo;

    public FileSaver(FileRepo fileRepo) {
        this.fileRepo = fileRepo;
    }

    public void saveItems(ManageEntity manageEntity, String type, Long receiverId, Long senderId) {

        if (manageEntity.getVideo() != null) {
            var video = manageEntity.getVideo();
            fileRepo.save(File.builder().fileId(video.getFileId()).type("video").receiverId(receiverId)
                    .senderId(senderId).receiverType(type).build());
        }

        if (manageEntity.getDocument() != null) {
            var video = manageEntity.getDocument();
            fileRepo.save(File.builder().fileId(video.getFileId()).type("doc").receiverId(receiverId)
                    .senderId(senderId).receiverType(type).build());
        }
    }
}
