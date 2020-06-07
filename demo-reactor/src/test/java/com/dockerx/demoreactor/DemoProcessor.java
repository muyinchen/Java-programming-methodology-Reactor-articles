package com.dockerx.demoreactor;

import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.*;
import reactor.util.concurrent.Queues;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Author  知秋
 * @email fei6751803@163.com
 * @time Created by Auser on 2018/5/23 23:33.
 */
public class DemoProcessor {
    @Test
    public void unicastProcessor_test() {
        UnicastProcessor<Object> unicastProcessor = UnicastProcessor.create();
        FluxSink<Object> sink = unicastProcessor.sink();
        sink.next("aaa");
        sink.next("aaa");
        sink.next("aaa");
        sink.next("aaa");
        Flux.just("Hello", "DockerX").subscribe(unicastProcessor);
        unicastProcessor.subscribe(System.out::println);
       // unicastProcessor.subscribe(System.out::println); /*只允许一个订阅者,打开注释执行会报错*/
        sink.next("aaa");
    }
    @Test
    public void directProcessor_test() {
        DirectProcessor<Object> directProcessor = DirectProcessor.create();
        FluxSink<Object> sink = directProcessor.sink();
        sink.next("000");
        directProcessor.subscribe(System.out::println);
        sink.next("xxx");
        directProcessor.subscribe(System.out::println);
        sink.next("aaa");
        Flux.just("Hello", "DockerX").subscribe(directProcessor);
        sink.next("bbb");
        sink.next("bbb");
        directProcessor.subscribe(System.out::println);
        sink.next("ccc");
    }
    @Test
    public void emitterProcessor_test() {
        EmitterProcessor<Object> emitterProcessor = EmitterProcessor.create();
        FluxSink<Object> sink = emitterProcessor.sink();
        sink.next("000");
        Disposable subscribe = emitterProcessor.subscribe(System.out::println);
        sink.next("xxx");
        Disposable subscribe1 = emitterProcessor.subscribe(System.out::println);
        sink.next("aaa");
        Flux.just("Hello", "DockerX").subscribe(emitterProcessor);
        sink.next("bbb");
        Disposable subscribe2 = emitterProcessor.subscribe(System.out::println);
        sink.next("ccc");
        subscribe.dispose();
        subscribe1.dispose();
        subscribe2.dispose();
        sink.next("ddd");
    }
    @Test
    public void replayProcessor_test() {
        ReplayProcessor<Object> replayProcessor = ReplayProcessor.create();
        Flux.just("Hello", "DockerX").subscribe(replayProcessor);
        FluxSink<Object> sink = replayProcessor.sink();
        sink.next("000");
        Disposable subscribe = replayProcessor.subscribe(System.out::println);
        sink.next("xxx");
        Disposable subscribe1 = replayProcessor.subscribe(System.out::println);
        sink.next("aaa");
        Flux.just("Hello", "DockerX").subscribe(replayProcessor);
        sink.next("bbb");
        Disposable subscribe2 = replayProcessor.subscribe(System.out::println);
        sink.next("ccc");
        subscribe.dispose();
        subscribe1.dispose();
        subscribe2.dispose();
        sink.next("ddd");
        System.out.println("###################");
        Disposable subscribe3 = replayProcessor.subscribe(System.out::println);
    }

    @Test
    public void topicProcessor_test() throws InterruptedException {
        //TopicProcessor<Object> topicProcessor = TopicProcessor.create();
        TopicProcessor<Object> topicProcessor = TopicProcessor.share("share",Queues.SMALL_BUFFER_SIZE);
        //TopicProcessor<Object> topicProcessor = TopicProcessor.share("share",2);
        //Flux.interval(Duration.ofSeconds(1)).subscribe(topicProcessor);
        //Flux.just("Hello", "DockerX").subscribe(topicProcessor);
        //Disposable subscribe0 = topicProcessor.subscribe(System.out::println);
        //FluxSink<Object> sink = topicProcessor.sink();
        Flux.just("Hello", "DockerX").subscribe(topicProcessor);
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        AtomicInteger c = new AtomicInteger();

        topicProcessor.onNext("000");
        Disposable subscribe = topicProcessor.subscribe(t -> System.out.println(t + " ZERO " + a.incrementAndGet()));
        topicProcessor.onNext("xxx");
        Disposable subscribe1 = topicProcessor.subscribe(t -> System.out.println(t + " One " + b.incrementAndGet()));
        topicProcessor.onNext("aaa");
       // Flux.just("Hello", "DockerX").subscribe(topicProcessor);
        topicProcessor.onNext("bbb");
        Disposable subscribe2 = topicProcessor.subscribe(t -> System.out.println(t + " Two " + c.incrementAndGet()));
        topicProcessor.onNext("ccc");
        //subscribe.dispose();
        //subscribe1.dispose();
        //subscribe2.dispose();
        topicProcessor.onNext("ddd");
        topicProcessor.onNext("eee");
        topicProcessor.onNext("fff");
        topicProcessor.onNext("fff");
       // topicProcessor.dispose();
        Flux.just("Hello", "DockerX").doOnNext(System.out::println).subscribe(topicProcessor);
        topicProcessor.subscribe(t -> System.out.println(t + " Four "));
        Flux.just("Hello1", "DockerX1").subscribe(topicProcessor);
        Thread.sleep(10000);
    }
    @Test
    public void topicProcessor_coldSource_test() throws InterruptedException {

        TopicProcessor<Object> topicProcessor = TopicProcessor.share("share",Queues.SMALL_BUFFER_SIZE);
        Disposable subscribe0 = topicProcessor.subscribe(t -> System.out.println(t + " XXX " ));
        Flux.just("Hello", "DockerX").subscribe(topicProcessor);
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        AtomicInteger c = new AtomicInteger();

       // Thread.sleep(1000);
        topicProcessor.onNext("000");
        Disposable subscribe = topicProcessor.subscribe(t -> System.out.println(t + " ZERO " + a.incrementAndGet()));
        topicProcessor.onNext("xxx");
        Disposable subscribe1 = topicProcessor.subscribe(t -> System.out.println(t + " One " + b.incrementAndGet()));
        topicProcessor.onNext("aaa");

        topicProcessor.onNext("bbb");
        Disposable subscribe2 = topicProcessor.subscribe(t -> System.out.println(t + " Two " + c.incrementAndGet()));
        topicProcessor.onNext("ccc");

        topicProcessor.onNext("ddd");
        topicProcessor.onNext("eee");
        topicProcessor.onNext("fff");
        topicProcessor.onNext("fff");

        Flux.just("Hello", "DockerX").doOnNext(System.out::println).subscribe(topicProcessor);
        topicProcessor.subscribe(t -> System.out.println(t + " Four "));
        Thread.sleep(10000);
    }
    @Test
    public void testProcessingMessages() throws Exception {
        int numberOfMessages = 1000;

        TopicProcessor<Integer> processor = TopicProcessor.share("share",Queues.SMALL_BUFFER_SIZE);
        FluxSink<Integer> sink = processor.sink();

        ExecutorService executorService = Executors.newFixedThreadPool(4);

        Queue<Integer> messages = new ConcurrentLinkedQueue<>();

        processor.subscribe(messages::add);

        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < numberOfMessages; i++) {
            executorService.submit(() -> sink.next(counter.incrementAndGet())
            );
        }

        Thread.sleep(10000);
        System.out.println(messages.size());
        assertEquals(numberOfMessages, messages.size());
    }

    @Test
    public void testProcessingMessages2() throws Exception {
        int numberOfMessages = 1000;

        TopicProcessor<Integer> processor = TopicProcessor.share("share",Queues.SMALL_BUFFER_SIZE);

        ExecutorService executorService = Executors.newFixedThreadPool(4);

        Queue<Integer> messages = new ConcurrentLinkedQueue<>();

        processor.subscribe(messages::add);

        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < numberOfMessages; i++) {
            executorService.submit(() -> processor.onNext(counter.incrementAndGet())
            );
        }

        Thread.sleep(10000);
        System.out.println(messages.size());
        assertEquals(numberOfMessages, messages.size());
    }


    @Test
    public void testWorkQueueProcessor() throws InterruptedException {
        WorkQueueProcessor<Integer> workQueueProcessor = WorkQueueProcessor.create();

        workQueueProcessor.subscribe(e -> System.out.println("One subscriber: "+e));
        workQueueProcessor.subscribe(e -> System.out.println("Two subscriber: "+e));
        workQueueProcessor.subscribe(e -> System.out.println("Three subscriber: "+e));

        IntStream.range(1,20)
                 .forEach(workQueueProcessor::onNext);
        Thread.sleep(10000);

    }

    @Test
    public void advancedCold() {
        Flux<String> source = Flux.fromIterable(Arrays.asList("blue", "green", "orange", "purple"))
                                  .doOnNext(System.out::println)
                                  .filter(s -> s.startsWith("o"))
                                  .map(String::toUpperCase);
        source.subscribe(d -> System.out.println("Subscriber 1: "+d));
        source.subscribe(d -> System.out.println("Subscriber 2: "+d));
    }

    @Test
    public void hot() {
        EmitterProcessor<String> hotSource = EmitterProcessor.create();

        hotSource.subscribe(d -> System.out.println("Subscriber 1 to Hot Source: "+d));
        hotSource.onNext("blue");
        hotSource.onNext("green");
        hotSource.subscribe(d -> System.out.println("Subscriber 2 to Hot Source: "+d));
        hotSource.onNext("orange");
        hotSource.onNext("purple");
        hotSource.onComplete();
    }
    @Test
    public void advancedHot() {
        UnicastProcessor<String> hotSource = UnicastProcessor.create();
        Flux<String> hotFlux = hotSource.publish()
                                        .autoConnect()
                                        .map(String::toUpperCase);
        hotFlux.subscribe(d -> System.out.println("Subscriber 1 to Hot Source: "+d));
        hotSource.onNext("blue");
        hotSource.onNext("green");
        hotFlux.subscribe(d -> System.out.println("Subscriber 2 to Hot Source: "+d));
        hotSource.onNext("orange");
        hotSource.onNext("purple");
        hotSource.onComplete();
    }

    public static Unsafe getUnsafeInstance() throws Exception{
        Field unsafeStaticField =
                Unsafe.class.getDeclaredField("theUnsafe");
        unsafeStaticField.setAccessible(true);
        return (Unsafe) unsafeStaticField.get(null);
    }


    @Test
    public void unsafeInstance_test() throws Exception {
        Unsafe u = getUnsafeInstance();
        //Unsafe u = Unsafe.getUnsafe();
        int[] arr = {1,2,3,4,5,6,7,8,9,10};

        int b = u.arrayBaseOffset(int[].class);

        int s = u.arrayIndexScale(int[].class);

        u.putInt(arr, (long)b+s*9, 1);

        for(int i=0;i<10;i++){

            int v = u.getInt(arr, (long)b+s*i);

            System.out.print(v+" ");

        }
    }
    static DemoProcessor de(){
        return new DemoProcessor();
    }
    @Test
    public void unsafeInstance_test2() {

        int[] arr = {1,2,3,4,5,6,7,8,9,10};

        int[] b = arr;
        arr=null;
        System.out.println(Arrays.toString(b));
        System.out.println(de());
        System.out.println(de());

    }
}
