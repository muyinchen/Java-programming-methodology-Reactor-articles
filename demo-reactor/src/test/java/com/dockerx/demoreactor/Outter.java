package com.dockerx.demoreactor;

import org.junit.Test;

/**
 * @author Author  知秋
 * @email fei6751803@163.com
 * @time Created by Auser on 2018/5/13 22:12.
 */
public class Outter {
    int a=10;
    static int b =5;
    public Outter() {

    }

    static class Inner {

        String bbb= "vvvvvv";
        String abc;
        Inner() {
            this.abc="aaaa";
        }
    }

    public String getA() {
        Inner inner = new Inner();
        return inner.abc;
    }

    @Test
    public void dummy() {
        System.out.println(new Outter().getA());

    }
}
