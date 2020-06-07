package com.dockerx.demoreactor.edu;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author: ZhiQiu
 * @email: fei6751803@163.com
 * @date: 2019/3/26 22:53.
 */
public class Student implements BiFunction<Student,Function<Student,Student>,Teacher> {

    private Function<Student,Teacher> teacherBiFunction;
    private Predicate<Student> predicate;

    private Grade grade;

    private Mark mark;
    /**
     * Books有对应的Function<Student,Student>实现，
     * 这样，只要有这个字段也就同时又了这个Function逻辑，
     * 属于一箭双雕的玩法，同样，此类中如果有教师这个字段，
     * 也可以给教师设定一个Function<Student,Student>实现，
     * 这样就可以做到灵活应用，所以在只需要books对应的
     * Function<Student,Student>逻辑的时候，无须在构造器处专门传入此Function实现，
     * 这也是这种设计的核心，将特殊实现外包即可，即books与teacher都可能有各自的Function实现
     * 此为球员自带体系设计模式
     */
    private Books books;

    public Student(Function<Student, Teacher> teacherBiFunction, Predicate<Student> predicate) {
        this.teacherBiFunction = teacherBiFunction;
        this.predicate = predicate;
    }

    public Student(Grade grade, Mark mark,Books books) {
        this.grade = grade;
        this.mark = mark;
        this.books =books;
    }

    public Books getBooks() {
        return books;
    }

    public Student setBooks(Books books) {
        this.books = books;
        return this;
    }

    public Grade getGrade() {
        return grade;
    }

    public Mark getMark() {
        return mark;
    }

    public Student setMark(Mark mark) {
        this.mark = mark;
        return this;
    }

    public Student setGrade(Grade grade) {
        this.grade = grade;
        return this;
    }


    @Override
    public Teacher apply(Student student, Function<Student, Student> studentStudentFunction) {
         if (this.predicate!=null&&this.predicate.test(student)) {
             if (teacherBiFunction != null){
                 Student student1 = studentStudentFunction.apply(student);
                    return teacherBiFunction.apply(student1);
             }
        }
        return new Teacher(Grade.UNASSIGNED,books);
    }
}
