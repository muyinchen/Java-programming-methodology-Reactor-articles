package com.dockerx.demoreactor.edu;


import java.util.function.Function;

/**
 * @author: ZhiQiu
 * @email: fei6751803@163.com
 * @date: 2019/3/26 22:54.
 * 此处的Function<Student,Student>属于外包策略，
 * 专门针对Books的一种逻辑，假如教师有特殊实现也可以设定一个
 */
public class Books implements Function<Student,Student> {

    private String name;

    public Books() {
    }

    public Books(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }


    @Override
    public Student apply(Student student) {
        if (student.getGrade()==Grade.ONE){
            return student;
        }
        return new Student(Grade.UNASSIGNED,new Mark(0),new Books("不及格大学士"));
    }
}
