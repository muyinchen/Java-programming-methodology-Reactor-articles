package com.dockerx.demoreactor;

import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.*;
import reactor.test.StepVerifier;

/**
 * @author Author  知秋
 * @email fei6751803@163.com
 * @time Created by Auser on 2018/6/26 1:23.
 */
public class DemoHooksTrace {
    @Test
    public void testTrace() {
        Hooks.onOperatorDebug();
        try {
            Mono.fromCallable(() -> {
                throw new RuntimeException();
            })
                .map(d -> d)
                .block();
        }
        catch(Exception e){
            e.printStackTrace();
            Assert.assertTrue(e.getSuppressed()[0].getMessage().contains("MonoCallable"));
            return;
        }
        finally {
            Hooks.resetOnOperatorDebug();
        }
        // throw new IllegalStateException();
    }
    @Test
    public void testTrace1() {
        Hooks.onOperatorDebug();
        try {
            Mono.fromCallable(() -> {
                System.out.println("1");
                return 1;
            })
                .map(d -> d)
                .subscribe(e->{throw new RuntimeException();});
        }
        catch(Exception e){
            e.printStackTrace();
            Assert.assertTrue(e.getSuppressed()[0].getMessage().contains("MonoCallable"));
            return;
        }
        finally {
            Hooks.resetOnOperatorDebug();
        }
        // throw new IllegalStateException();
    }


 /*  @Test
    public void lastOperatorTest() {
        Hooks.onLastOperator(Operators.lift((sc, sub) ->
                new CoreSubscriber<>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        sub.onSubscribe(s);
                    }

                    @Override
                    public void onNext(Object o) {
                        sub.onNext(((Integer) o) + 1);
                    }

                    @Override
                    public void onError(Throwable t) {
                        sub.onError(t);
                    }

                    @Override
                    public void onComplete() {
                        sub.onComplete();
                    }
                }));

        StepVerifier.create(Flux.just(1, 2, 3)
                                .log()
                                .log())
                    .expectNext(2, 3, 4)
                    .verifyComplete();

        StepVerifier.create(Mono.just(1)
                                .log()
                                .log())
                    .expectNext(2)
                    .verifyComplete();

        StepVerifier.create(ParallelFlux.from(Mono.just(1), Mono.just(1))
                                        .log()
                                        .log())
                    .expectNext(2, 2)
                    .verifyComplete();

        Hooks.resetOnLastOperator();
    }

    @Test
    public void lastOperatorFilterTest() {
        Hooks.onLastOperator(Operators.lift(sc -> sc.tags()
                                                    .anyMatch(t -> t.getT1()
                                                                    .contains("metric")),
                (sc, sub) -> new CoreSubscriber<>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        sub.onSubscribe(s);
                    }

                    @Override
                    public void onNext(Object o) {
                        sub.onNext(((Integer) o) + 1);
                    }

                    @Override
                    public void onError(Throwable t) {
                        sub.onError(t);
                    }

                    @Override
                    public void onComplete() {
                        sub.onComplete();
                    }
                }));

        StepVerifier.create(Flux.just(1, 2, 3)
                                .tag("metric", "test")
                                .log()
                                .log())
                    .expectNext(2, 3, 4)
                    .verifyComplete();

        StepVerifier.create(Mono.just(1)
                                .tag("metric", "test")
                                .log()
                                .log())
                    .expectNext(2)
                    .verifyComplete();

        StepVerifier.create(ParallelFlux.from(Mono.just(1), Mono.just(1))
                                        .tag("metric", "test")
                                        .log()
                                        .log())
                    .expectNext(2, 2)
                    .verifyComplete();

        StepVerifier.create(Flux.just(1, 2, 3)
                                .log()
                                .log())
                    .expectNext(1, 2, 3)
                    .verifyComplete();

        StepVerifier.create(Mono.just(1)
                                .log()
                                .log())
                    .expectNext(1)
                    .verifyComplete();

        StepVerifier.create(ParallelFlux.from(Mono.just(1), Mono.just(1))
                                        .log()
                                        .log())
                    .expectNext(1, 1)
                    .verifyComplete();

        Hooks.resetOnLastOperator();
    }*/

    /*@Test
    public void eachOperatorTest() {
        Hooks.onEachOperator(Operators.lift((sc, sub) ->
                new CoreSubscriber<>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        sub.onSubscribe(s);
                    }

                    @Override
                    public void onNext(Object o) {
                        sub.onNext(((Integer) o) + 1);
                    }

                    @Override
                    public void onError(Throwable t) {
                        sub.onError(t);
                    }

                    @Override
                    public void onComplete() {
                        sub.onComplete();
                    }
                }));

        StepVerifier.create(Flux.just(1, 2, 3)
                                .log()
                                .log())
                    .expectNext(4, 5, 6)
                    .verifyComplete();

        StepVerifier.create(Mono.just(1)
                                .log()
                                .log())
                    .expectNext(4)
                    .verifyComplete();

        StepVerifier.create(ParallelFlux.from(Mono.just(1), Mono.just(1))
                                        .log()
                                        .log())
                    .expectNext(6, 6)
                    .verifyComplete();

        Hooks.resetOnEachOperator();
    }*/


    @Test
    public void checkpointTest() {
        Flux.just(1, 0)
            .map(x -> 1 / x)
            .checkpoint("test")
            .subscribe(System.out::println);
    }

    @Test
    public void logTest() {
        Flux.range(1, 2)
            .log("Test")
            .subscribe(System.out::println);
    }
}
