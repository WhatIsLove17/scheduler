package ru.whatislove.scheduler.services.telegram;


import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.whatislove.scheduler.repository.StudentRepo;
import ru.whatislove.scheduler.repository.TeacherRepo;
import ru.whatislove.scheduler.repository.UserScheduleRepo;
import ru.whatislove.scheduler.services.telegram.util.ManageEntity;

@Service
public class CommandService {

    private final StudentRepo studentRepo;
    private final TeacherRepo teacherRepo;
    private final UserScheduleRepo userScheduleRepo;
    private final StudentCommandService studentCommandService;
    private final TeacherCommandService teacherCommandService;

    public CommandService(StudentRepo studentRepo, TeacherRepo teacherRepo,
                          UserScheduleRepo userScheduleRepo, StudentCommandService studentCommandService,
                          TeacherCommandService teacherCommandService) {
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
        this.userScheduleRepo = userScheduleRepo;
        this.studentCommandService = studentCommandService;
        this.teacherCommandService = teacherCommandService;
    }

    @Transactional
    public List<ManageEntity> executeCommand(String command, long chatId) {

        var studentOpt = studentRepo.findStudentByChatId(chatId);
        var teacherOpt = teacherRepo.findTeacherByChatId(chatId);

        if (command.equals("/start")) {
            if (studentOpt.isPresent()) {
                userScheduleRepo.deleteAllByStudentId(studentOpt.get().getId());
                studentRepo.deleteStudentByChatId(chatId);
            }
            else if (teacherOpt.isPresent()) {
                var teacher = teacherOpt.get();
                userScheduleRepo.deleteAllByTeacherId(teacher.getId());
                teacher.setChatId(null);
                teacherRepo.save(teacher);
            }

            return startCommand(chatId);
        }

        if (studentOpt.isPresent()) {
            return studentCommandService.studentBranch(command, studentOpt.get());
        }
        else if (teacherOpt.isPresent()){
            return teacherCommandService.teacherBranch(command, teacherOpt.get());
        }
        else {
            return registerCommand(command, chatId);
        }
    }

    private List<ManageEntity> startCommand(long chatId) {

        var studentButton = KeyboardButton.builder()
                .text("Я учащийся").build();

        var teacherButton = KeyboardButton.builder()
                .text("Я преподаватель").build();

        var keyboardRow = new KeyboardRow();
        keyboardRow.addAll(List.of(studentButton, teacherButton));

        var keyboard = ReplyKeyboardMarkup.builder().keyboardRow(keyboardRow).oneTimeKeyboard(true).build();

        return List.of(new ManageEntity(chatId,"Привет, я - Фернандо.\nА кто ты?", keyboard));
    }

    private List<ManageEntity> registerCommand(String command, long chatId) {
        switch (command) {
            case "Я учащийся" -> {
                return studentCommandService.registerStudentCommand(chatId, new ArrayList<>());
            }
            case "Я преподаватель" -> {
                return teacherCommandService.registerTeacherCommand(chatId);
            }
            case default -> {
                return teacherCommandService.saveTeacherChatId(chatId, command);
            }
        }
    }

}
