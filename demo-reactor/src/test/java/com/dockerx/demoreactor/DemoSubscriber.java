package com.dockerx.demoreactor;

import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

/**
 * @author Author  知秋
 * @email fei6751803@163.com
 * @time Created by Auser on 2018/5/9 1:22.
 */
public class DemoSubscriber<T> extends BaseSubscriber<T> {
    public void hookOnSubscribe(Subscription subscription) {
        System.out.println("Subscribed");
        request(1);
    }

    public void hookOnNext(T value) {
        System.out.println(value);
        request(1);
    }
}
