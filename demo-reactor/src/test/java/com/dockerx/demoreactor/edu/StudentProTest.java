package com.dockerx.demoreactor.edu;

import org.junit.Test;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * @author: ZhiQiu
 * @email: fei6751803@163.com
 * @date: 2019/3/27 0:56.
 */
public class StudentProTest {

    @Test
    public void getTeacher() {
        BiFunction<Function<StudentPro, StudentPro>, StudentPro, Teacher> biFunction = (func, stu) -> {
            StudentPro student = func.apply(stu);
            System.out.println(student.getGrade());
            if (student.getMark().getMarkponit() > 80) {
                return new Teacher(student.getGrade(), student.getBooks());
            }
            return new Teacher(student.getGrade(), new Books("刚及格小教授"));
        };


        Predicate<StudentPro> predicate = student -> student.getGrade() != Grade.ONE;

        StudentPro student = new StudentPro(biFunction, predicate);
        student.setGrade(Grade.THREE).setMark(new Mark(90)).setBooks(new Books("蛤蟆功"));
        Teacher apply = student.getTeacher(student);
        System.out.println(apply.getBooks().getName());

    }
}