package com.dockerx.demoreactor;

import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Author  知秋
 * @email fei6751803@163.com
 * @time Created by Auser on 2018/5/19 23:07.
 */
public class DemoOperator {
    @Test
    public void filter_test() {
        Flux.range(1, 10)
            .filter(i -> i % 2 == 0)
            .subscribe(System.out::println);
    }

    @Test
    public void advancedCompose() {
        Function<Flux<String>, Flux<String>> filterAndMap =
                f -> f.filter(color -> !color.equals("orange"))
                      .map(String::toUpperCase);

        Flux.fromIterable(Arrays.asList("blue", "green", "orange", "purple"))
            .doOnNext(System.out::println)
            .transform(filterAndMap)
            .subscribe(d -> System.out.println("Subscriber to Transformed MapAndFilter: " + d));
    }

    @Test
    public void advancedTransform() {
        AtomicInteger ai = new AtomicInteger();
        Function<Flux<String>, Flux<String>> filterAndMap = f -> {
            if (ai.incrementAndGet() == 1) {
                return f.filter(color -> !color.equals("orange"))
                        .map(String::toUpperCase);
            }
            return f.filter(color -> !color.equals("purple"))
                    .map(String::toUpperCase);
        };

        Flux<String> composedFlux =
                Flux.fromIterable(Arrays.asList("blue", "green", "orange", "purple"))
                    .doOnNext(System.out::println)
                    .compose(filterAndMap);

        composedFlux.subscribe(d -> System.out.println("Subscriber 1 to Composed MapAndFilter :" + d));
        composedFlux.subscribe(d -> System.out.println("Subscriber 2 to Composed MapAndFilter: " + d));
    }

    @Test
    public void buffer_test() {
        Flux.range(1, 40).buffer(20).subscribe(System.out::println);
        System.out.println("+++++++++++++++++++++");
        Flux.range(1, 10).bufferUntil(i -> i % 2 == 0).subscribe(System.out::println);
        Flux.range(1, 10).bufferWhile(i -> i % 2 == 0).subscribe(System.out::println);
        Flux.interval(Duration.ofSeconds(1))
            .buffer(Duration.ofSeconds(10))
            .take(2)
            .toStream()
            .forEach(System.out::println);
    }

    @Test
    public void window_test() {
        Flux.range(1, 40).window(20).subscribe(System.out::println);
        System.out.println("############");
        Flux.range(1, 4).windowUntil(i -> i % 2 == 0).subscribe(System.out::println);
        System.out.println("############");
        Flux.range(1, 4).windowWhile(i -> i % 2 == 0).subscribe(System.out::println);
        System.out.println("############");

        Stream<Flux<Long>> fluxStream = Flux.interval(Duration.ofSeconds(1))
                                            .window(Duration.ofSeconds(4))
                                            .take(2)
                                            .toStream();
        fluxStream.forEach(x -> x.toStream().forEach(System.out::print));

    }

    @Test
    public void window_test2() {
        Flux.range(1, 40).window(20).
                subscribe(x ->
                        x.subscribe(System.out::print,
                                e -> System.out.println(e.getMessage()),
                                () -> System.out.println("DONE!")));
        System.out.println("############");
        Flux.range(1, 4).windowUntil(i -> i % 2 == 0).subscribe(x ->
                x.subscribe(System.out::print,
                        e -> System.out.println(e.getMessage()),
                        () -> System.out.println("DONE!")));
        System.out.println("############");
        Flux.range(1, 4).windowWhile(i -> i % 2 == 0).subscribe(x ->
                x.subscribe(System.out::print,
                        e -> System.out.println(e.getMessage()),
                        () -> System.out.println("DONE!")));
        System.out.println("############");

        Stream<Flux<Long>> fluxStream = Flux.interval(Duration.ofSeconds(1))
                                            .window(Duration.ofSeconds(4))
                                            .take(2)
                                            .toStream();
        fluxStream.forEach(x -> x.toStream().forEach(System.out::print));

    }

    @Test
    public void buffer_window_test() {
        Flux.range(1, 30)
            .buffer(10, 5)
            .subscribe(System.out::println);
        System.out.println("+++++++++++++++++++++");
        Flux.range(1, 10)
            .window(5, 3)
            .toStream()
            .forEach(x -> x.subscribe(t -> System.out.print(t + " "),
                    System.out::println,
                    () -> System.out.println("end")));
    }

    @Test
    public void group_test() {
        Flux.just(1, 3, 5, 2, 4, 6, 11, 12, 13)
            .groupBy(i -> i % 2 == 0 ? "even" : "odd")
            .concatMap(g -> g.defaultIfEmpty(-1) //if empty groups, show them
                             .map(String::valueOf) //map to string
                             .startWith(g.key())) //start with the group's key
            .subscribe(t -> System.out.print(t + " "));
    }

    @Test
    public void group_test1() {
        Flux.just(1, 3, 5, 2, 4, 6, 11, 12, 13)
            .groupBy(i -> i % 2 == 0 ? "even" : "odd")
            .subscribe(t -> System.out.print(t + " "));
    }
    @Test
    public void group_test2() {
        Flux.just(1, 3, 5, 2, 4, 6, 11, 12, 13)
            .groupBy(i -> i % 2 == 0 ? "even" : "odd")
            .publishOn(Schedulers.parallel())
            .subscribe(t ->{
                System.out.println("Key: "+ t.key());
                t.subscribe(System.out::print,e-> System.out.println(e.getMessage()),() -> System.out.println("Done!"));
            });
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void merge_test() {
        Flux.merge(Flux.interval(Duration.ZERO, Duration.ofMillis(100)).take(5),
                Flux.interval(Duration.ofMillis(50), Duration.ofMillis(100)).take(5))
            .toStream()
            .forEach(t -> System.out.print(t + " "));
        System.out.println();
        Flux.mergeSequential(Flux.interval(Duration.ZERO, Duration.ofMillis(100)).take(5),
                Flux.interval(Duration.ofMillis(50), Duration.ofMillis(100)).take(5))
            .toStream()
            .forEach(t -> System.out.print(t + " "));
    }

    @Test
    public void flatMap_test() {
        Flux.just(5, 10)
            .flatMap(x ->
                    Flux.interval(Duration.ofMillis(x * 10), Duration.ofMillis(100))
                        .map(i -> x + ": " + i).take(x - 3))
            .toStream()
            .forEach(t -> System.out.print(t + " "));
        System.out.println();
        Flux.just(5, 10)
            .flatMapSequential(x ->
                    Flux.interval(Duration.ofMillis(x * 10), Duration.ofMillis(100))
                        .map(i -> x + ": " + i).take(x - 3))
            .toStream()
            .forEach(t -> System.out.print(t + " "));
    }

    @Test
    public void concatMap_test() {
        Flux.just(5, 10)
            .concatMap(x ->
                    Flux.interval(Duration.ofMillis(x * 10), Duration.ofMillis(100))
                        .map(i -> x + ": " + i).take(x - 3))
            .toStream()
            .forEach(t -> System.out.print(t + " "));
    }

    @Test
    public void concatMap_test_multi() {
        Random random = new Random();
        Flux.just(5, 10,11,8)
            .concatMap(x ->
                    Flux.interval(Duration.ofMillis(random.nextInt(500)))
                        .map(i -> x + ": " + i).take(x - 3).doOnNext(d -> System.out.println(Thread.currentThread().getName() + " OnNext: " + d)))
           // .doOnNext(x -> System.out.println(Thread.currentThread().getName() + " OnNext: " + x))
            .subscribe(t -> {

                try {
                    Thread.sleep(random.nextInt(100));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(t + " ");
            });
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void combineLatest_test() {
        Flux.combineLatest(Flux.interval(Duration.ofSeconds(2))
                               .map(x -> "Java" + x).take(5),
                Flux.interval(Duration.ofSeconds(1))
                    .map(x -> "Spring" + x).take(5),
                (s, f) -> f + ":" + s)
            .toStream()
            .forEach(System.out::println);
    }

    @Test
    public void advancedConnectable() throws InterruptedException {
        Flux<Long> source = Flux.interval(Duration.ofSeconds(2))
                                .doOnSubscribe(s -> System.out.println("subscribed to source"));

        //ConnectableFlux<Long> co = source.publish();
        ConnectableFlux<Long> co = source.replay(2);

        co.subscribe(t -> System.out.println(t + " One"), e -> {
        }, () -> {
        });
        co.subscribe(t -> System.out.println(t + " Two"), e -> {
        }, () -> {
        });

        System.out.println("done subscribing");
        Thread.sleep(500);
        System.out.println("will now connect");

        co.connect();
        //Thread.sleep(5000);
        Thread.sleep(10000);
        co.subscribe(t -> System.out.println(t + " Three"), e -> {
        }, () -> {
        });
        Thread.sleep(5000);
    }

    @Test
    public void advancedConnectablepro() throws InterruptedException {
        Flux<Long> source = Flux.interval(Duration.ofSeconds(1))
                                .doOnSubscribe(s -> System.out.println("subscribed to source"));

        //Flux<Long> co = source.publish().refCount(3);
        Flux<Long> co = source.publish().refCount(3, Duration.ofSeconds(3));

        Disposable subscribe =
                co.subscribe(t ->
                        System.out.println(t + " One"), System.out::println, () -> {
                });

        co.subscribe(t -> System.out.println(t + " Two"), System.out::println, () -> {
        });
        Thread.sleep(3000);
        co.subscribe(t -> System.out.println(t + " Three"), System.out::println, () -> {
        });
        Thread.sleep(3000);
        subscribe.dispose();

        Thread.sleep(10000);
        co.subscribe(t -> System.out.println(t + " Four"), System.out::println, () -> {
        });

        Thread.sleep(100000);
    }

    @Test
    public void advancedConnectablepro2() throws InterruptedException {
        Flux<Long> source = Flux.interval(Duration.ofSeconds(1))
                                .doOnSubscribe(s -> System.out.println("subscribed to source"));

        Flux<Long> co = source.publish().refCount(3, Duration.ofSeconds(3));

        Disposable one =
                co.subscribe(t ->
                        System.out.println(t + " One"), System.out::println, () -> {
                });

        final Disposable two = co.subscribe(t -> System.out.println(t + " Two"), System.out::println, () -> {
        });
        Thread.sleep(3000);
        Disposable three = co.subscribe(t -> System.out.println(t + " Three"), System.out::println, () -> {
        });

        Thread.sleep(3000);
        one.dispose();

        Thread.sleep(3000);
        two.dispose();
        three.dispose();
        //Thread.sleep(3000);  // <1>
        Thread.sleep(2000);
        co.subscribe(t -> System.out.println(t + " Four"), System.out::println, () -> {
        });

        Thread.sleep(5000);
    }

    @Test
    public void advancedConnectablepro3() throws InterruptedException {
        Flux<Long> source = Flux.interval(Duration.ofSeconds(1))
                                .doOnSubscribe(s -> System.out.println("subscribed to source"));

        Flux<Long> co = source.publish().refCount(3);

        Disposable one =
                co.subscribe(t ->
                        System.out.println(t + " One"), System.out::println, () -> {
                });

        final Disposable two = co.subscribe(t -> System.out.println(t + " Two"), System.out::println, () -> {
        });
        Thread.sleep(3000);
        Disposable three = co.subscribe(t -> System.out.println(t + " Three"), System.out::println, () -> {
        });

        Thread.sleep(3000);
        one.dispose();

        Thread.sleep(3000);
        two.dispose();
        three.dispose();
        //Thread.sleep(3000);  // <1>
        Thread.sleep(2000);
        co.subscribe(t -> System.out.println(t + " Four"), System.out::println, () -> {
        });

        Thread.sleep(5000);
    }


    @Test
    public void advancedConnectablepro_publish() throws InterruptedException {
        Flux<Long> source = Flux.interval(Duration.ofSeconds(1))
                                .doOnSubscribe(s -> System.out.println("subscribed to source"));

       // Flux<Long> co = source.doOnNext(t-> System.out.println("The item is :"+t)).publish(3).autoConnect();
       // ConnectableFlux<Long> co = source.doOnNext(t-> System.out.println("The item is :"+t)).publish(3);
       // co.connect();
        Flux<Long> co = source.doOnNext(t-> System.out.println("The item is :"+t))
                              .publish(2)
                              .refCount();

        Disposable subscribe =
                co.subscribe(t ->
                        System.out.println(t + " One"), System.out::println, () -> {
                },s ->s.request(5));


        co.subscribe(t -> System.out.println(t + " Two"), System.out::println, () -> {
        },s ->s.request(6));

        co.subscribe(t -> System.out.println(t + " Three"), System.out::println, () -> {
        },s ->s.request(7));

        co.subscribe(t -> System.out.println(t + " Four"), System.out::println, () -> {
        },s ->s.request(8));
        System.out.println("main thread wait for a moment");

        Thread.sleep(6000);
        System.out.println("sleep is done");
        subscribe.dispose();
        Thread.sleep(100000);
    }

    @Test
    public void advancedConnectablepro_publish2() throws InterruptedException {
        Flux<Long> source = Flux.interval(Duration.ofSeconds(1))
                                .doOnSubscribe(s -> System.out.println("subscribed to source"));

         Flux<Long> co = source.doOnNext(t-> System.out.println("The item is :"+t)).publish(3).autoConnect(4);
        // ConnectableFlux<Long> co = source.doOnNext(t-> System.out.println("The item is :"+t)).publish(3);
        // co.connect();
        //Flux<Long> co = source.doOnNext(t-> System.out.println("The item is :"+t))
        //                      .publish(2)
        //                      .refCount();

        Disposable subscribe =
                co.subscribe(t ->
                        System.out.println(t + " One"), System.out::println, () -> {
                },s ->s.request(50));


        co.subscribe(t -> System.out.println(t + " Two"), System.out::println, () -> {
        },s ->s.request(60));

        co.subscribe(t -> System.out.println(t + " Three"), System.out::println, () -> {
        },s ->s.request(70));

        co.subscribe(t -> System.out.println(t + " Four"), System.out::println, () -> {
        },s ->s.request(80));
        System.out.println("main thread wait for a moment");

        Thread.sleep(6000);
        System.out.println("sleep is done");
        subscribe.dispose();
        Thread.sleep(100000);
    }

}
