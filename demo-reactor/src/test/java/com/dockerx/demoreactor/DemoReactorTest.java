package com.dockerx.demoreactor;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.test.StepVerifier;
import reactor.test.StepVerifierOptions;
import reactor.test.publisher.PublisherProbe;
import reactor.test.publisher.TestPublisher;
import reactor.test.scheduler.VirtualTimeScheduler;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Author  知秋
 * @email fei6751803@163.com
 * @time Created by Auser on 2018/6/19 2:32.
 */
public class DemoReactorTest {
    public <T> Flux<T> appendCustomError(Flux<T> source) {
        return source.concatWith(Mono.error(new IllegalArgumentException("custom")));
    }
    @Test
    public void testAppendBoomError() {
        Flux<String> source = Flux.just("foo", "bar");

        StepVerifier.create(
                appendCustomError(source))
                    .expectNext("foo")
                    .expectNext("bar")
                    .expectErrorMessage("custom");
                   // .verify();
    }
    @Test
    public void errorHandlingIntervalMillisNotContinued() throws InterruptedException {
        VirtualTimeScheduler virtualTimeScheduler = VirtualTimeScheduler.create();
        VirtualTimeScheduler.set(virtualTimeScheduler);
        Flux<String> flux =
                Flux.interval(Duration.ofMillis(250))
                    .map(input -> {
                        if (input < 3) return "tick " + input;
                        throw new RuntimeException("boom");
                    })
                    .onErrorReturn("Uh oh");
        flux.subscribe(System.out::println);
        //Thread.sleep(2100); // <1>
        virtualTimeScheduler.advanceTimeBy(Duration.ofHours(1));
        StepVerifier.withVirtualTime(() -> flux, () -> virtualTimeScheduler, Long.MAX_VALUE)
                    .thenAwait(Duration.ofSeconds(3))
                    .expectNext("tick 0")
                    .expectNext("tick 1")
                    .expectNext("tick 2")
                    .expectNext("Uh oh")
                    .verifyComplete();
    }


    //interface MyEventListener<T> {
    //    void onDataChunk(List<T> chunk);
    //    void processComplete();
    //}
    //
    //interface MyEventProcessor {
    //    void register(DemoRecatorFluxTest.MyEventListener<String> eventListener);
    //    void dataChunk(String... values);
    //    void processComplete();
    //}

    private DemoReactorFluxTest.MyEventProcessor myEventProcessor = new DemoReactorFluxTest.MyEventProcessor() {

        private DemoReactorFluxTest.MyEventListener<String> eventListener;
        private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        @Override
        public void register(DemoReactorFluxTest.MyEventListener<String> eventListener) {
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
                    new DemoReactorFluxTest.MyEventListener<String>() { // <1>
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
        StepVerifier.withVirtualTime(() -> bridge)
                    .expectNoEvent(Duration.ofSeconds(10))
                    .expectSubscription()
                    .expectNoEvent(Duration.ofSeconds(10))
                    .then(() -> myEventProcessor.dataChunk("foo", "bar", "baz"))
                    .expectNext("foo", "bar", "baz")
                    .expectNoEvent(Duration.ofSeconds(10))
                    .then(() -> myEventProcessor.processComplete())
                    .verifyComplete();
    }

    @Test
    public void expectNoEventTest() {
        StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofHours(4), Duration.ofDays(1)).take(2))
                    .expectSubscription()
                    .expectNoEvent(Duration.ofHours(4))
                    .expectNext(0L)
                    .thenAwait(Duration.ofDays(1))
                    .expectNext(1L)
                    .expectNoEvent(Duration.ofHours(8))
                    .verifyComplete();
    }

    @Test
    public void assertDroppedElementsAllPass() {
        StepVerifier.create(Flux.from(s -> {
            s.onSubscribe(Operators.emptySubscription());
            s.onNext("foo");
            s.onComplete();
            s.onNext("bar");
            s.onNext("baz");
        }).take(3))
                    .expectNext("foo")
                    .expectComplete()
                    .verifyThenAssertThat()
                    .hasDroppedElements()
                    .hasDropped("baz")
                    .hasDroppedExactly("baz", "bar");
    }
    @Test
    public void assertNotDroppedElementsFailureOneDrop() {
        try {
            StepVerifier.create(Flux.from(s -> {
                s.onSubscribe(Operators.emptySubscription());
                s.onNext("foo");
                s.onComplete();
                s.onNext("bar");
            }).take(2))
                        .expectNext("foo")
                        .expectComplete()
                        .verifyThenAssertThat()
                        .hasNotDroppedElements();
            fail("expected an AssertionError");
        }
        catch (AssertionError ae) {
            assertThat(ae).hasMessage("Expected no dropped elements, found <[bar]>.");
        }
    }

    @Test
    public void assertDroppedElementsFailureOneExtra() {
        try {
            StepVerifier.create(Flux.from(s -> {
                s.onSubscribe(Operators.emptySubscription());
                s.onNext("foo");
                s.onComplete();
                s.onNext("bar");
                s.onNext("baz");
            }).take(3))
                        .expectNext("foo")
                        .expectComplete()
                        .verifyThenAssertThat()
                        .hasDropped("foo");
            fail("expected an AssertionError");
        }
        catch (AssertionError ae) {
            assertThat(ae).hasMessage("Expected dropped elements to contain <[foo]>, was <[bar, baz]>.");
        }
    }

    @Test
    public void assertDroppedElementsFailureOneMissing() {
        try {
            StepVerifier.create(Flux.from(s -> {
                s.onSubscribe(Operators.emptySubscription());
                s.onNext("foo");
                s.onComplete();
                s.onNext("bar");
                s.onNext("baz");
            }).take(3))
                        .expectNext("foo")
                        .expectComplete()
                        .verifyThenAssertThat()
                        .hasDroppedExactly("baz");
            fail("expected an AssertionError");
        }
        catch (AssertionError ae) {
            assertThat(ae).hasMessage("Expected dropped elements to contain exactly <[baz]>, was <[bar, baz]>.");
        }
    }

    @Test
    public void assertDroppedErrorAllPass() {
        Throwable err1 = new IllegalStateException("boom1");
        Throwable err2 = new IllegalStateException("boom2");
        StepVerifier.create(Flux.from(s -> {
            s.onSubscribe(Operators.emptySubscription());
            s.onError(err1);
            s.onError(err2);
        }).buffer(1))
                    .expectError()
                    .verifyThenAssertThat()
                    .hasDroppedErrors()
                    .hasDroppedErrors(1)
                    .hasDroppedErrorOfType(IllegalStateException.class)
                    .hasDroppedErrorWithMessageContaining("boom")
                    .hasDroppedErrorWithMessage("boom2")
                    .hasDroppedErrorMatching(t -> t instanceof IllegalStateException && "boom2".equals(t.getMessage()));
    }

    @Test
    public void assertDroppedErrorFailureWrongType() {
        try {
            Throwable err1 = new IllegalStateException("boom1");
            Throwable err2 = new IllegalStateException("boom2");
            StepVerifier.create(Flux.from(s -> {
                s.onSubscribe(Operators.emptySubscription());
                s.onError(err1);
                s.onError(err2);
            }).buffer(1))
                        .expectError()
                        .verifyThenAssertThat()
                        .hasDroppedErrorOfType(IllegalArgumentException.class);
            fail("expected an AssertionError");
        }
        catch (AssertionError ae) {
            assertThat(ae).hasMessage("Expected dropped error to be of type java.lang.IllegalArgumentException, was java.lang.IllegalStateException.");
        }
    }

    @Test
    public void withInitialContext() {
        StepVerifier.create(Mono.subscriberContext(),
                StepVerifierOptions.create().withInitialContext(Context.of("foo", "bar")))
                    .assertNext(c -> Assertions.assertThat(c.getOrDefault("foo", "baz"))
                                               .isEqualTo("bar"))
                    .verifyComplete();
    }

    @Test
    public void withInitialContextButNoPropagation() {
        StepVerifier.create(Mono.just(1)
                            //    .map(i->i)
                                            ,
                StepVerifierOptions.create().withInitialContext(Context.of("foo", "bar")))
                    .expectNoAccessibleContext()
                    .expectNext(1)
                    .verifyComplete();
    }

    @Test
    public void withInitialContextAndContextAssertionsParents() {
        StepVerifier.create(Mono.just(1).map(i -> i + 10),
                StepVerifierOptions.create().withInitialContext(Context.of("foo", "bar")))
                    .expectAccessibleContext()
                    .contains("foo", "bar")
                    .then()
                    .expectNext(11)
                    .verifyComplete();
    }

    @Test
    public void normalDisallowsNull() {
        TestPublisher<String> publisher = TestPublisher.create();

        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> publisher.next(null))
                .withMessage("emitted values must be non-null");
    }

    @Test
    public void misbehavingAllowsNull() {
        TestPublisher<String> publisher = TestPublisher.createNoncompliant(TestPublisher.Violation.ALLOW_NULL);

        StepVerifier.create(publisher)
                    .then(() -> publisher.emit("foo", null))
                    .expectNext("foo", null)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void normalDisallowsOverflow() {
        TestPublisher<String> publisher = TestPublisher.create();

        StepVerifier.create(publisher, 1)
                    .then(() -> publisher.next("foo")).as("should pass")
                    .then(() -> publisher.emit("bar")).as("should fail")
                    .expectNext("foo")
                    .expectErrorMatches(e -> e instanceof IllegalStateException &&
                            "Can't deliver value due to lack of requests".equals(e.getMessage()))
                    .verify();

        publisher.assertNoRequestOverflow();
    }

    @Test
    public void misbehavingAllowsOverflow() {
        TestPublisher<String> publisher = TestPublisher.createNoncompliant(TestPublisher.Violation.REQUEST_OVERFLOW);

        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> StepVerifier.create(publisher, 1)
                                              .then(() -> publisher.emit("foo", "bar"))
                                              .expectNext("foo")
                                              .expectNext("bar")
                                              //.expectComplete() //n/a
                                              .expectError()
                                              .verify())
                .withMessageContaining("expected production of at most 1;");

        publisher.assertRequestOverflow();
    }

    /*@Test
    public void gh1236_test() {
        TestPublisher<String> result = TestPublisher.createCold();

        assertThat(result.emit("value").mono().block()).isEqualTo("value");
    }*/

    public Flux<String> processOrFallback(Mono<String> source, Publisher<String> fallback) {
        return source
                .flatMapMany(phrase -> Flux.fromArray(phrase.split("\\s+")))
                .switchIfEmpty(fallback);
    }

    @Test
    public void testSplitPathIsUsed() {
        StepVerifier.create(processOrFallback(Mono.just("just a  phrase with    tabs!"),
                Mono.just("EMPTY_PHRASE")))
                    .expectNext("just", "a", "phrase", "with", "tabs!")
                    .verifyComplete();
    }

    @Test
    public void testEmptyPathIsUsed() {
        StepVerifier.create(processOrFallback(Mono.empty(), Mono.just("EMPTY_PHRASE")))
                    .expectNext("EMPTY_PHRASE")
                    .verifyComplete();
    }



    private Mono<String> executeCommand(String command) {
        return Mono.just(command + " DONE");
    }
    public Mono<Void> processOrFallback(Mono<String> commandSource, Mono<Void> doWhenEmpty) {
        return commandSource
                .flatMap(command -> executeCommand(command).then()) // <1>
                .switchIfEmpty(doWhenEmpty); // <2>
    }
    @Test
    public void testCommandEmptyPathIsUsedBoilerplate() {
        AtomicBoolean wasInvoked = new AtomicBoolean();
        AtomicBoolean wasRequested = new AtomicBoolean();
        Mono<Void> testFallback = Mono.<Void>empty()
                .doOnSubscribe(s -> wasInvoked.set(true))
                .doOnRequest(l -> wasRequested.set(true));
        processOrFallback(Mono.empty(), testFallback).subscribe();
        assertThat(wasInvoked.get()).isTrue();
        assertThat(wasRequested.get()).isTrue();
    }
    @Test
    public void testCommandEmptyPathIsUsed() {
        PublisherProbe<Void> probe = PublisherProbe.empty(); // <1>
        StepVerifier.create(processOrFallback(Mono.empty(), probe.mono())) // <2>
                    .verifyComplete();
        probe.assertWasSubscribed(); //<3>
        probe.assertWasRequested(); //<4>
        probe.assertWasNotCancelled(); //<5>
    }
}
