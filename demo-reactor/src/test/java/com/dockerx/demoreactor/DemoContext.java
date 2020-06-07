package com.dockerx.demoreactor;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Optional;

/**
 * @author Author  知秋
 * @email fei6751803@163.com
 * @time Created by Auser on 2018/6/14 0:51.
 */
public class DemoContext {
    @Test
    public void contextSimple1() {
        String key = "message";
        Mono<String> r = Mono.just("Hello")
                             .flatMap( s -> Mono.subscriberContext() //<2>
                                                .map( ctx -> s + " " + ctx.get(key))) //<3>
                             .subscriberContext(ctx -> ctx.put(key, "World")); //<1>
        r.subscribe(System.out::println);
        StepVerifier.create(r)
                    .expectNext("Hello World") //<4>
                    .verifyComplete();
    }

    @Test
    public void contextSimple2() {
        String key = "message";
        Mono<String> r = Mono.subscriberContext() // <1>
                             .map( ctx -> ctx.put(key, "Hello")) // <2>
                             .flatMap( ctx -> Mono.subscriberContext()) // <3>
                             .map( ctx -> ctx.getOrDefault(key,"Default")); // <4>
        r.subscribe(System.out::println);

        StepVerifier.create(r)
                    .expectNext("Default") // <5>
                    .verifyComplete();
    }


    private static final String HTTP_CORRELATION_ID = "reactive.http.library.correlationId";
    Mono<Tuple2<Integer, String>> doPut(String url, Mono<String> data) {
        Mono<Tuple2<String, Optional<Object>>> dataAndContext =
                data.zipWith(Mono.subscriberContext()
                                 .map(c -> c.getOrEmpty(HTTP_CORRELATION_ID)));
        return dataAndContext
                .<String>handle((dac, sink) -> {
                    if (dac.getT2().isPresent()) {
                        sink.next("PUT <" + dac.getT1() + "> sent to " + url + " with header X-Correlation-ID = " + dac.getT2().get());
                    }
                    else {
                        sink.next("PUT <" + dac.getT1() + "> sent to " + url);
                    }
                    sink.complete();
                })
                .map(msg -> Tuples.of(200, msg));
    }
    Mono<Tuple2<Integer, String>> doPut0(String url, Mono<String> data) {
        Mono<Tuple2<String, Optional<Object>>> dataAndContext =
                data.zipWith(Mono.subscriberContext()
                                 .map(c -> c.getOrEmpty(HTTP_CORRELATION_ID)));
        return dataAndContext
                .map(dac -> {
                    if (dac.getT2().isPresent()) {
                       return "PUT <" + dac.getT1() + "> sent to " + url + " with header X-Correlation-ID = " + dac.getT2().get();
                    }
                    else {
                        return "PUT <" + dac.getT1() + "> sent to " + url;
                    }
                })
                .map(msg -> Tuples.of(200, msg));
    }

    @Test
    public void contextForLibraryReactivePut() {
        Mono<String> put = doPut("www.example.com", Mono.just("Walter"))
                .subscriberContext(Context.of(HTTP_CORRELATION_ID, "2-j3r9afaf92j-afkaf"))
                .filter(t -> t.getT1() < 300)
                .map(Tuple2::getT2);
        StepVerifier.create(put)
                    .expectNext("PUT <Walter> sent to www.example.com with header X-Correlation-ID = 2-j3r9afaf92j-afkaf")
                    .verifyComplete();
    }
    @Test
    public void contextForLibraryReactivePutNoContext() {
        Mono<String> put = doPut("www.example.com", Mono.just("Walter"))
                .filter(t -> t.getT1() < 300)
                .map(Tuple2::getT2);
        StepVerifier.create(put)
                    .expectNext("PUT <Walter> sent to www.example.com")
                    .verifyComplete();
    }
}
