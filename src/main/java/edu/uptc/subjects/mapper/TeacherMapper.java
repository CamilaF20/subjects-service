package edu.uptc.subjects.mapper;

import edu.uptc.subjects.dto.TeacherRequest;
import edu.uptc.subjects.dto.TeacherResponse;
import edu.uptc.subjects.entity.Teacher;
import org.springframework.stereotype.Component;

@Component
public class TeacherMapper {

    public Teacher toEntity(TeacherRequest request) {
        return new Teacher(request.firstName(), request.lastName(), request.email());
    }

    public TeacherResponse toResponse(Teacher teacher) {
        return new TeacherResponse(teacher.getId(), teacher.getFirstName(), teacher.getLastName(), teacher.getEmail());
    }
}