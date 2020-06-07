package com.dockerx.demoreactor.edu;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author: ZhiQiu
 * @email: fei6751803@163.com
 * @date: 2019/3/27 0:44.
 * 属于球队集中式设计
 */
public class StudentPro implements Function<StudentPro, StudentPro> {
    private BiFunction<Function<StudentPro, StudentPro>, StudentPro, Teacher> teacherBiFunction;
    private Predicate<StudentPro> predicate;

    private Grade grade;

    private Mark mark;
    private Books books;

    public StudentPro(BiFunction<Function<StudentPro, StudentPro>, StudentPro, Teacher> teacherBiFunction,
                      Predicate<StudentPro> predicate) {
        this.teacherBiFunction = teacherBiFunction;
        this.predicate = predicate;
    }


    public StudentPro(Grade grade, Mark mark, Books books) {
        this.grade = grade;
        this.mark = mark;
        this.books = books;
    }

    public Books getBooks() {
        return books;
    }

    public StudentPro setBooks(Books books) {
        this.books = books;
        return this;
    }

    public Grade getGrade() {
        return grade;
    }

    public Mark getMark() {
        return mark;
    }

    public StudentPro setMark(Mark mark) {
        this.mark = mark;
        return this;
    }

    public StudentPro setGrade(Grade grade) {
        this.grade = grade;
        return this;
    }

    public Teacher getTeacher(StudentPro student) {
        if (this.predicate != null && this.predicate.test(student)) {
            if (teacherBiFunction != null) {

                return teacherBiFunction.apply(this, student);
            }
        }
        return new Teacher(Grade.UNASSIGNED, new Books("不及格小教师"));
    }

    @Override
    public StudentPro apply(StudentPro student) {
        if (student.getGrade() == Grade.THREE) {
            return student;
        }
        return new StudentPro(Grade.UNASSIGNED, new Mark(0), new Books("不及格大学士"));
    }
}
