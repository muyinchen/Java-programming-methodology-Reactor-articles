package com.dockerx.demoreactor;

import org.junit.Test;
import org.reactivestreams.Subscription;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * @author Author  知秋
 * @email fei6751803@163.com
 * @time Created by Auser on 2018/5/3 21:39.
 */
public class DemoReactorFluxTest {

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void log(Object msg) {
        sleep(20);
        System.out.println(
                Thread.currentThread().getName() +
                        ": " + msg);
    }

    @Test
    public void flux_create() {
        Flux.create(emitter -> {

            for (int i = 0; i <= 1000; i++) {
                if (emitter.isCancelled()) {
                    return;
                }
                System.out.println("Source	created	" + i);
                int finalI = i;
                new Thread(() -> {
                    emitter.next(finalI +100);
                    sleep(100);
                }).start();
                emitter.next(i);
            }
        }).doOnNext(s -> System.out.println("Source	pushed	" + s))
            .subscribeOn(Schedulers.single())
            .subscribe(customer -> {
                sleep(10);
                System.out.println("所获取到的Customer的Id为: " + customer);
            });
        sleep(10000);
    }

    @Test
    public void flux_createXXX() {
        Flux.create(emitter -> {

            for (int i = 0; i <= 1000; i++) {
                if (emitter.isCancelled()) {
                    return;
                }
                System.out.println("Source	created	" + i);
                int finalI = i;
                new Thread(() -> {
                    sleep(100);
                    emitter.next(finalI +100);
                }).start();
                emitter.next(i);
            }
        }).doOnNext(s -> System.out.println("Source	pushed	" + s))
            .subscribeOn(Schedulers.parallel())
            .subscribe(customer -> {
                sleep(10);
                System.out.println("所获取到的Customer的Id为: " + customer);
            });
        sleep(10000);
    }

    @Test
    public void flux_create2() {
        Flux.create(emitter -> {

            for (int i = 0; i <= 1000; i++) {
                if (emitter.isCancelled()) {
                    return;
                }
                System.out.println("Source	created	" + i);
                emitter.next(i);
            }
        }).doOnNext(s -> System.out.println("Source	pushed	" + s))
            .subscribeOn(Schedulers.newElastic("aaa"))
            .subscribe(customer -> {
                sleep(1000);
                System.out.println("所获取到的Customer的Id为: " + customer);
            });
        sleep(10000);
    }


    public static Scheduler custom_Scheduler() {
        ThreadFactory threadFactory = new CustomizableThreadFactory("自定义线程池--");

        Executor executor = new ThreadPoolExecutor(
                10,  //corePoolSize
                10,  //maximumPoolSize
                0L, TimeUnit.MILLISECONDS, //keepAliveTime, unit
                new LinkedBlockingQueue<>(1000),  //workQueue
                threadFactory
        );
        return Schedulers.fromExecutor(executor);
    }

    @Test
    public void flux_generate() {
        final Random random = new Random();
        Flux.generate(ArrayList::new, (list, sink) -> {
            int value = random.nextInt(100);
            list.add(value);
            sink.next(value);
            if (list.size() == 5) {
                sink.complete();
            }
            return list;
        }).doOnNext(item -> System.out.println("emitted	on	thread	"
                + Thread.currentThread().getName() + "  " + item))
            .publishOn(Schedulers.parallel())

            .map(x -> String.format("[%s] %s", Thread.currentThread().getName(), x))
            .publishOn(Schedulers.elastic())
            .map(x -> String.format("[%s] %s", Thread.currentThread().getName(), x))
            .subscribeOn(Schedulers.parallel())
            .subscribeOn(custom_Scheduler()).subscribe(DemoReactorFluxTest::log);
        sleep(10000);
    }

    @Test
    public void flux_generate2() {
        final Random random = new Random();
        Flux.generate(ArrayList::new, (list, sink) -> {
            int value = random.nextInt(100);
            list.add(value);
            // change the state value in the sink.next
             new Thread(() -> sink.next(value)).start();
            new Thread(() -> sink.next(value)).start();

            //sink.next(value);
            if (list.size() == 5) {
                sink.complete();
            }
            return list;
        }).doOnNext(item -> System.out.println("emitted	on	thread	"
                + Thread.currentThread().getName() + "  " + item))
            .subscribeOn(Schedulers.parallel())
            .parallel().runOn(Schedulers.parallel())
            .map(x -> String.format("[%s] %s", Thread.currentThread().getName(), x))
            .subscribe(DemoReactorFluxTest::log);
        sleep(10000);
    }

    @Test
    public void flux_generate3() {
        final Random random = new Random();
        Flux.generate(ArrayList::new, (list, sink) -> {
            int value = random.nextInt(100);
            list.add(value);
            System.out.println("所发射元素产生的线程: "+Thread.currentThread().getName());
            sink.next(value);
     //       sink.next(2);
            sleep(1000);
            if (list.size() == 20) {
                sink.complete();
            }
            return list;
        }).publishOn(custom_Scheduler(),1)
            .map(x -> String.format("[%s] %s", Thread.currentThread().getName(), x))
            .subscribe(DemoReactorFluxTest::log);
        sleep(20000);
    }

    @Test
    public void flux_generate4() {
        final Random random = new Random();
        Flux.generate(ArrayList::new, (list, sink) -> {
            int value = random.nextInt(100);
            list.add(value);
            System.out.println("所发射元素产生的线程: "+Thread.currentThread().getName());
            sink.next(value);
            sleep(1000);
            if (list.size() == 20) {
                sink.complete();
            }
            return list;
        }).publishOn(custom_Scheduler(),1)
            .map(x -> String.format("[%s] %s", Thread.currentThread().getName(), x))
            .subscribe(System.out::println);
        sleep(20000);
    }


    public static <T> T delayCalculation0(T value) {
      //  sleep(ThreadLocalRandom.current().nextInt(1000));
        return value;
    }

    public static <T> T delayCalculation(T value) {
        sleep(ThreadLocalRandom.current().nextInt(1000));
        System.out.println(
                Thread.currentThread().getName() +
                        ": " + value);
        return value;
    }

    @Test
    public void custom_Scheduler_test1() {
        Flux.create(sink -> {
            for (int i = 0; i < 10; i++) {
                sink.next("1 " + Thread.currentThread().getName() + " 2");
            }
            sink.complete();
        })
            .publishOn(custom_Scheduler())
            .publishOn(Schedulers.parallel())
            .subscribe(DemoReactorFluxTest::log);
        sleep(10000);
    }

    @Test
    public void custom_Scheduler_test2() {
        Flux.range(1, 10)
            .parallel().runOn(Schedulers.parallel())
            .map(x -> String.format("[%s] %s", Thread.currentThread().getName(), x))
            .subscribe(System.out::println);
        sleep(10000);
    }

    @Test
    public void flux_publishOn_subscribeOn() {
        Scheduler scheduler = Schedulers.fromExecutor(Executors.newFixedThreadPool(10));
        Flux.create(sink -> {
            for (int i = 0; i < 10; i++) {
                sink.next(i+" $");
                System.out.println(
                        Thread.currentThread().getName());
          //      sleep(1000);
            }
            sink.complete();
        })
           // .doOnNext(x -> System.out.println(Thread.currentThread().getName() + "111111"))
            .publishOn(Schedulers.parallel())
            .map(DemoReactorFluxTest::delayCalculation0)
            .doOnNext(x -> System.out.println(Thread.currentThread().getName() + "111111"))
            .map(x -> String.format("[%s] %s", Thread.currentThread().getName()+" first", x))
            .publishOn(Schedulers.parallel())
            .map(x -> String.format("[%s] %s", Thread.currentThread().getName()+" Ss", x))
            .subscribe(msg->System.out.println(
                    Thread.currentThread().getName() +
                            ": " + msg));
        sleep(1000);
    }
    @Test
    public void flux_publishOn_subscribeOn1() {
        Scheduler scheduler = Schedulers.fromExecutor(Executors.newFixedThreadPool(10));
        Flux.create(sink -> {
            for (int i = 0; i < 10; i++) {
                sink.next(i+" $");
                System.out.println(
                        Thread.currentThread().getName());
                //      sleep(1000);
            }
            sink.complete();
        })
            // .doOnNext(x -> System.out.println(Thread.currentThread().getName() + "111111"))
            //.publishOn(Schedulers.parallel())
            .map(DemoReactorFluxTest::delayCalculation0)
            .doOnNext(x -> System.out.println(Thread.currentThread().getName() + "111111"))
            .map(x -> String.format("[%s] %s", Thread.currentThread().getName()+" first", x))
            .publishOn(Schedulers.elastic())
            .map(x -> String.format("[%s] %s", Thread.currentThread().getName()+" Ss", x))
            .subscribe(DemoReactorFluxTest::delayCalculation);
        sleep(1000);
    }

    @Test
    public void DemoSubscriber_test() {
        Flux.just("Hello", "DockerX").subscribe(new DemoSubscriber<>());
    }

    @Test
    public void DemoSubscriber_test2() {
        Flux.just("Hello", "DockerX").subscribe(new BaseSubscriber<>() {
            @Override
            protected void hookOnNext(String value) {
                System.out.println(value);
            }

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                System.out.println("Subscribed");
                requestUnbounded();
            }
        });
    }
    @Test
    public void DemoSubscriber_test_flux_generate3() {
        final Random random = new Random();
        Flux.generate(ArrayList::new, (list, sink) -> {
            int value = random.nextInt(100);
            list.add(value);
            System.out.println("所发射元素产生的线程: "+Thread.currentThread().getName());
            sink.next(value);
             sleep(2);
            if (list.size() == 6) {
                sink.complete();
            }
            return list;
        })
            .publishOn(Schedulers.elastic(),2)
          //  .publishOn(custom_Scheduler(),2)
            .map(x -> String.format("[%s] %s", Thread.currentThread().getName(), x))
            .subscribe(System.out::println);
        sleep(20000);
    }
    @Test
    public void flux_subscribeOn() {
        final Random random = new Random();
        Flux.create(sink -> {
           List list=new ArrayList<>();
            Integer i=0;
            while (list.size() != 100) {
                int value = random.nextInt(100);
                list.add(value);
                i=i+1;
                System.out.println("所发射元素产生的线程: "+Thread.currentThread().getName()+" num: "+i);
                sink.next(value);
                sleep(10);
            }
            sink.complete();

        }).doOnRequest(x-> System.out.println("...1"+Thread.currentThread().getName()))
           // .subscribeOn(Schedulers.elastic())
        .doOnRequest(x-> System.out.println("...2"+Thread.currentThread().getName()))
            .subscribeOn(Schedulers.elastic(),false)
            .publishOn(custom_Scheduler(),2)
            .map(x -> String.format("[%s] %s", Thread.currentThread().getName(), x))
            .subscribe(System.out::println);
        sleep(10000);
    }
    @Test
    public void flux_creates() {
        Flux.just("Hello", "DockerX").subscribe(System.out::println);
        Flux.fromArray(new Integer[]{1, 2, 3, 4}).subscribe(System.out::println);
        Flux.empty().subscribe(System.out::println);
        Flux.range(1, 10).subscribe(System.out::println);
        Flux.interval(Duration.ofMillis(1000)).subscribe(System.out::println);
        sleep(10000);
    }

    @Test
    public void stream_test() {

       Stream.of("1", "2", "3").parallel().map(s -> {
            System.out.println(Thread.currentThread().getName());
            return s.toUpperCase();
        }).forEach(System.out::println);

    }

    @Test
    public void mono_creates() {
        Mono.fromCallable(() -> "Hello DockerX").subscribe(System.out::println);
        Mono.fromSupplier(() -> "Hello DockerX").subscribe(System.out::println);
        Mono.justOrEmpty(Optional.of("Hello DockerX")).subscribe(System.out::println);
        Mono.create(sink -> sink.success("Hello DockerX")).subscribe(System.out::println);
        Mono.ignoreElements(Mono.fromCallable(() -> "Hello DockerX")).
                subscribe(System.out::println,System.out::println,()-> System.out.println("done"));
    }

    @Test
    public void cast() {
        Father sample = new Son();
        System.out.println("调用成员：  " + sample.name);
        System.out.print("调用方法： ");
        sample.method();

    }


    interface MyEventListener<T> {

        void onDataChunk(List<T> chunk);
        void processComplete();
    }

    interface MyEventProcessor {
        void register(MyEventListener<String> eventListener);
        void dataChunk(String... values);
        void processComplete();
    }

    private MyEventProcessor myEventProcessor = new MyEventProcessor() {

        private MyEventListener<String> eventListener;
        private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        @Override
        public void register(MyEventListener<String> eventListener) {
            this.eventListener = eventListener;
        }

        @Override
        public void dataChunk(String... values) {
            executor.schedule(() -> eventListener.onDataChunk(Arrays.asList(values)),
                    500, TimeUnit.MILLISECONDS);
        }

        @Override
        public void processComplete() {
            executor.schedule(() -> eventListener.processComplete(),
                    500, TimeUnit.MILLISECONDS);
        }
    };

    @Test
    public void producingCreate() {
        Flux<String> bridge = Flux.create(sink -> {
            myEventProcessor.register( // <4>
                    new MyEventListener<String>() { // <1>

                        public void onDataChunk(List<String> chunk) {
                            for(String s : chunk) {
                                sink.next(s); // <2>
                            }
                        }

                        public void processComplete() {
                            sink.complete(); // <3>
                        }
                    });
        });
        bridge.publishOn(Schedulers.elastic()).subscribe(System.out::println,System.out::println,()-> System.out.println("done!"));
        myEventProcessor.dataChunk("foo", "bar", "baz");
        myEventProcessor.processComplete();
        sleep(1000);
        StepVerifier.withVirtualTime(() -> bridge)
                   // .expectNoEvent(Duration.ofSeconds(10))
                    .consumeSubscriptionWith(s-> System.out.println("xxx "+s.toString()))
                  // .expectSubscription()
                    //.expectNoEvent(Duration.ofSeconds(10))
                    .then(() -> myEventProcessor.dataChunk("foo", "bar", "baz"))
                    .expectNext("foo", "bar", "baz")
                    .expectNoEvent(Duration.ofSeconds(10))
                    .then(() -> myEventProcessor.processComplete())
                    .verifyComplete();
    }



    public String alphabet(int letterNumber) {
        if (letterNumber < 1 || letterNumber > 26) {
            return null;
        }
        int letterIndexAscii = 'A' + letterNumber - 1;
        return "" + (char) letterIndexAscii;
    }
    @Test
    public void producingHandle() {
        Flux<String> alphabet = Flux.just(-1, 30, 13, 9, 20)
                                    .handle((i, sink) -> {
                                        String letter = alphabet(i); // <1>
                                        if (letter != null) // <2>
                                            sink.next(letter); // <3>
                                    });

        alphabet.subscribe(System.out::println);

        StepVerifier.create(alphabet)
                    .expectNext("M", "I", "T")
                    .verifyComplete();
    }


    //代码：
    //父类
    public class Father {
        protected String name = "父亲属性";

        public void method() {
            System.out.println("父类方法，对象类型：" + this.getClass());
        }

    }


    //子类
    public class Son extends Father {
        protected String name = "儿子属性";

        public void method() {
            System.out.println("子类方法，对象类型：" + this.getClass());
        }

    }

    @Test
    public void Outter_inner() {
        System.out.println(new Outter.Inner().abc);
        System.out.println(new Outter.Inner().bbb);
        System.out.println(new Outter().a);
    }
}