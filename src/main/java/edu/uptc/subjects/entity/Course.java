package edu.uptc.subjects.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false, length = 100)
    private String schedule;

    @Column(nullable = false, length = 100)
    private String period;

    @Column(nullable = false)
    private Integer capacity;

    protected Course() {
    }

    public Course(Subject subject, Teacher teacher, String schedule, String period, Integer capacity) {
        this.subject = subject;
        this.teacher = teacher;
        this.schedule = schedule;
        this.period = period;
        this.capacity = capacity;
    }

    public Long getId() {
        return id;
    }

    public Subject getSubject() {
        return subject;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getPeriod() {
        return period;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}