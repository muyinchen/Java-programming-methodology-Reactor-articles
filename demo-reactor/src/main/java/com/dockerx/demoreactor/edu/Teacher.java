package com.dockerx.demoreactor.edu;

/**
 * @author: ZhiQiu
 * @email: fei6751803@163.com
 * @date: 2019/3/26 22:54.
 */
public class Teacher {
    private Grade grade;
    private Books books;

    public Teacher(Grade grade,Books books) {
        this.grade = grade;
        this.books =books;
    }

    public Grade getGrade() {
        return grade;
    }

    public Teacher setGrade(Grade grade) {
        this.grade = grade;
        return this;
    }

    public Books getBooks() {
        return books;
    }

    public Teacher setBooks(Books books) {
        this.books = books;
        return this;
    }
}
