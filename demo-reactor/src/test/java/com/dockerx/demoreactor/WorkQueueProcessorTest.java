package com.dockerx.demoreactor;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.WorkQueueProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;

import static org.junit.Assert.assertEquals;


/**
 * @author Author  知秋
 * @email fei6751803@163.com
 * @time Created by Auser on 2018/6/9 2:22.
 */
public class WorkQueueProcessorTest {
    private static final int PRODUCER_LATENCY = 5;
    private static final int CONSUMER_LATENCY = 4;

    private static final int RINGBUFFER_SIZE = 64;

    private static final int INITAL_MESSAGES_COUNT = 10;
    private static final int PRODUCED_MESSAGES_COUNT = 1024;
    private static final int BURST_SIZE = 5;

    private LongAccumulator maxRingBufferPending;
    private WorkQueueProcessor<Object> processor;
    private ExecutorService producerExecutor;
    private AtomicLong droppedCount;

    @Before
    public void setup() {
        maxRingBufferPending = new LongAccumulator(Long::max, Long.MIN_VALUE);
        droppedCount = new AtomicLong(0);
        producerExecutor = Executors.newSingleThreadExecutor();
    }

    @Test
    public void test() throws Exception {
        processor = WorkQueueProcessor.create("test-processor", RINGBUFFER_SIZE);

        Flux.create(this::burstyProducer)
            .onBackpressureDrop(this::incrementDroppedMessagesCounter)
            .subscribe(processor);

        processor.doOnNext(x-> System.out.println(x+" One"))
            .map(this::complicatedCalculation)
            .subscribe(this::logConsumedValue);
        processor.doOnNext(x-> System.out.println(x+" Two"))
                .map(this::complicatedCalculation)
                .subscribe(this::logConsumedValue);
        processor.doOnNext(x-> System.out.println(x+" Three"))
                 .map(this::complicatedCalculation)
                 .subscribe(this::logConsumedValue);
        processor.doOnNext(x-> System.out.println(x+" Four"))
                 .map(this::complicatedCalculation)
                 .subscribe(this::logConsumedValue);

        waitForProducerFinish();

        System.out.println("\n\nMax ringbuffer pending: " + maxRingBufferPending.get());

        assertEquals(0, getDroppedMessagesCount());
    }


    private void waitForProducerFinish() throws InterruptedException {
        producerExecutor.shutdown();
        producerExecutor.awaitTermination(20, TimeUnit.SECONDS);
    }

    private void logConsumedValue(Object value) {
        //System.out.print(value + ",");
    }

    private long getDroppedMessagesCount() {
        return droppedCount.get();
    }

    private void incrementDroppedMessagesCounter(Object dropped) {
        System.out.println("\nDropped: " + dropped);
        droppedCount.incrementAndGet();
    }

    private Object complicatedCalculation(Object value) {
        maxRingBufferPending.accumulate(processor.getPending());
        sleep(CONSUMER_LATENCY);
        return value;
    }

    private void burstyProducer(FluxSink<Object> emitter) {
        producerExecutor.execute(burstyProducerRunnable(emitter
        ));
    }

    private Runnable burstyProducerRunnable(final FluxSink<Object> emitter) {
        return () -> {

            // Let's start with some messages to keep the ringbuffer from going total empty
            for (int i = 0; i < INITAL_MESSAGES_COUNT; ++i) {
                emitter.next("initial" + i);
            }

            for (int outer = 0; outer < WorkQueueProcessorTest.PRODUCED_MESSAGES_COUNT / WorkQueueProcessorTest.BURST_SIZE; ++outer) {
                for (int inner = 0; inner < WorkQueueProcessorTest.BURST_SIZE; ++inner) {
                    emitter.next(outer * WorkQueueProcessorTest.BURST_SIZE + inner);
                }
                sleep(PRODUCER_LATENCY * WorkQueueProcessorTest.BURST_SIZE);
            }
        };
    }

    private static void sleep(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
