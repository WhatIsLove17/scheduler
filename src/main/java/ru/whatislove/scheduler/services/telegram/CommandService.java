package ru.whatislove.scheduler.services.telegram;


import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.whatislove.scheduler.models.Student;
import ru.whatislove.scheduler.models.User;
import ru.whatislove.scheduler.repository.StudentRepo;
import ru.whatislove.scheduler.repository.TeacherRepo;
import ru.whatislove.scheduler.repository.UserRepo;
import ru.whatislove.scheduler.repository.UserScheduleRepo;
import ru.whatislove.scheduler.services.telegram.util.InlineButtonsBuilder;
import ru.whatislove.scheduler.services.telegram.util.ManageEntity;
import ru.whatislove.scheduler.services.telegram.util.UserCommand;

import java.util.List;

@Service
public class CommandService {

    private final UserRepo userRepo;
    private final StudentRepo studentRepo;
    private final TeacherRepo teacherRepo;
    private final UserScheduleRepo userScheduleRepo;
    private final StudentCommandService studentCommandService;
    private final TeacherCommandService teacherCommandService;

    public CommandService(UserRepo userRepo, StudentRepo studentRepo, TeacherRepo teacherRepo,
                          UserScheduleRepo userScheduleRepo, StudentCommandService studentCommandService,
                          TeacherCommandService teacherCommandService) {
        this.userRepo = userRepo;
        this.studentRepo = studentRepo;
        this.teacherRepo = teacherRepo;
        this.userScheduleRepo = userScheduleRepo;
        this.studentCommandService = studentCommandService;
        this.teacherCommandService = teacherCommandService;
    }

    @Transactional
    public List<ManageEntity> executeCommand(ManageEntity manageEntity, long chatId) {

        String command = manageEntity.getMessage();

        var userOpt = userRepo.findByChatId(chatId);

        if (userOpt.isPresent()) {

            var user = userOpt.get();

            if (user.getRole() == null) {
                return setRoleCommand(user, command);
            } else {
                if (user.getRole().equals("teacher")) {
                    if (command != null && command.equals("/start")) {
                        userScheduleRepo.deleteAllByUserId(user.getId());
                        userRepo.deleteById(user.getId());
                        return startCommand(chatId);
                    }

                    return teacherCommandService.teacherBranch(manageEntity, user.getRoleId() == null ? null :
                                    teacherRepo.findById(user.getRoleId()).get(), user);
                } else {
                    if (command != null && command.equals("/start")) {
                        userScheduleRepo.deleteAllByUserId(user.getId());
                        studentRepo.deleteById(user.getRoleId());
                        userRepo.deleteById(user.getId());
                        return startCommand(chatId);
                    }

                    return studentCommandService.studentBranch(manageEntity, studentRepo.findById(user.getRoleId()).get(),
                            user);
                }
            }
        } else {
            return startCommand(chatId);
        }
    }

    private List<ManageEntity> startCommand(long chatId) {

        var user = User.builder().chatId(chatId).build();
        userRepo.save(user);

        var keyboard = InlineButtonsBuilder.builder().addSeparateButton(
                InlineKeyboardButton.builder()
                        .text("Я учащийся")
                        .callbackData("/studentReg")
                        .build()).addSeparateButton(
                InlineKeyboardButton.builder()
                        .text("Я преподаватель")
                        .callbackData("/teacherReg").build()).build();

        return List.of(new ManageEntity(chatId, "Привет, я - BamBoo.\nА кто ты?", keyboard));
    }

    private List<ManageEntity> setRoleCommand(User user, String command) {

        return switch (UserCommand.fromString(command).getCommand()) {
            case TEACHER_REG -> {
                user.setRole("teacher");
                user.setWaitCommand("/waitKey");
                userRepo.save(user);
                yield teacherCommandService.registerTeacherCommand(user);
            }
            case STUDENT_REG -> {
                user.setRole("student");
                user.setWaitCommand("/waitCity");
                var student = studentRepo.save(new Student());
                user.setRoleId(student.getId());
                userRepo.save(user);
                yield studentCommandService.registerStudentCommand(user);
            }
            default -> throw new RuntimeException();
        };
    }

}
