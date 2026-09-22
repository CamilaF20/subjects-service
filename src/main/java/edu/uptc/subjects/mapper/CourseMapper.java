package edu.uptc.subjects.mapper;

import edu.uptc.subjects.dto.CourseRequest;
import edu.uptc.subjects.dto.CourseResponse;
import edu.uptc.subjects.entity.Course;
import edu.uptc.subjects.entity.Subject;
import edu.uptc.subjects.entity.Teacher;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public Course toEntity(CourseRequest request, Subject subject, Teacher teacher) {
        return new Course(subject, teacher, request.schedule(), request.period(), request.capacity());
    }

    public CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getSubject().getId(),
                course.getTeacher().getId(),
                course.getSchedule(),
                course.getPeriod(),
                course.getCapacity());
    }
}