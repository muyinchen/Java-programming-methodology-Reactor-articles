package com.dockerx.demoreactor.edu;

import org.junit.Assert;
import org.junit.Test;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * @author: ZhiQiu
 * @email: fei6751803@163.com
 * @date: 2019/3/26 23:35.
 */
public class StudentTest {

    @Test
    public void apply() {

        Function<Student, Teacher> biFunction = student -> {
            //System.out.println(student.getGrade());
            if (student.getMark().getMarkponit() > 80) {
                return new Teacher(student.getGrade(), student.getBooks());
            }
            return new Teacher(student.getGrade(), new Books("刚及格大学士"));
        };

        Predicate<Student> predicate = student -> student.getGrade() != Grade.ONE;

        Student student = new Student(biFunction, predicate);
        student.setGrade(Grade.THREE)
               .setMark(new Mark(90))
               .setBooks(new Books("蛤蟆功"));
        //Teacher apply = student.apply(student, new Books());
        //System.out.println(apply.getBooks().getName());

        Assert.assertTrue("刚及格大学士".equals(student.apply(student, student.getBooks())
                                                 .getBooks()
                                                 .getName()));
    }
}