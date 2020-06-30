## The exploration of HttpHandler
In the previous chapters, we have known about the details of the design and implementation of Reactor-Netty. This involves `reactor.netty.http.server.HttpServer#handle`. And exactly say it is a `SPI(Service Provider Interface) `. It provides `BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler`. So that, for different situations, we can implement different handlers. Spring WebFlux and Reactor-Netty both have their own implementations. or Spring WebFlux, it needs to adapt with Spring Web, it made a lot of adaption designs. And this process is much complicated.
But for Reactor-Netty, it provides simple and flexible implementations for us. So in this section, we will start with implementations in Reactor-Netty. Then we can move to Spring WebFlux.

## The role of HttpServerRoutes
When we send `HTTP` requests to the backend Server side, it'll involve several types of requests such as `get`,`put`,`post` and `head`. It will also includes the request URL. Based on the type of requests and the URL, Server side will provide the specified service, then it will handle with your request. So, can we extract the process that find the service from this to become a Service Router?

So in `Reactor-Netty`, the author designed a `HttpServerRoutes` interface. This interface inherits from `BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>`. And it's used for handling route requests. When requests come in, then we do a lookup in our collection to find a suitable rule which match the type of this request. Once it matches, then we will invoke the `handler` which is responsible to deal with it. In `HttpServerRoutes` interface, it designed specified router rules for different types of requests such as `GET`, `POST`, `PUT`, `HEAD`, `DELETE` and so on. (For details, please check the following source code)

When we use this, at first we'll invoke `HttpServerRoutes#newRoutes` to get an instance of `DefaultHttpServerRoutes`. Then we put our router rules into this. For the design of router rules, basically we just use a collection to manage these rules into a collection. When requests come in, then we do a lookup in this collection to find the matched rule. Thus, the core method of this is `reactor.netty.http.server.HttpServerRoutes#route`. After we designed the router rule, then we can design the functional implementations for each rule by using `BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>`. Finally, when the request matches the rule, then we can invoke our implementation of `BiFunction` to handle the request.

```java
//reactor.netty.http.server.HttpServerRoutes
public interface HttpServerRoutes extends
                                  BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {

	static HttpServerRoutes newRoutes() {
		return new DefaultHttpServerRoutes();
	}


	default HttpServerRoutes delete(String path,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		return route(HttpPredicate.delete(path), handler);
	}

    ...

	default HttpServerRoutes get(String path,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		return route(HttpPredicate.get(path), handler);
	}

	default HttpServerRoutes head(String path,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		return route(HttpPredicate.head(path), handler);
	}

	default HttpServerRoutes index(final BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		return route(INDEX_PREDICATE, handler);
	}

	default HttpServerRoutes options(String path,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		return route(HttpPredicate.options(path), handler);
	}

	default HttpServerRoutes post(String path,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		return route(HttpPredicate.post(path), handler);
	}

	default HttpServerRoutes put(String path,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		return route(HttpPredicate.put(path), handler);
	}

	HttpServerRoutes route(Predicate<? super HttpServerRequest> condition,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler);

	...
}
```
For the design of router rules，as I mentioned before, we can use a List to store these rules in our implementation class of `HttpServerRoutes` which is `DefaultHttpServerRoutes`. The next thing we need to do is to put every rule into this list. Since what we only do is just adding elements into this list, so we don't need to return anything. So, we can use `Consumer<? super HttpServerRoutes>` to deal with this process. For the rule matching, it's basically predication for conditions. So, we can use `Predicate<? super HttpServerRequest>` to do this，Since we have designed different `BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>` for different rules，So we can combine them together. Thus, in Reactor-Netty, we'll have `reactor.netty.http.server.DefaultHttpServerRoutes.HttpRouteHandler`.

```java
//reactor.netty.http.server.DefaultHttpServerRoutes.HttpRouteHandler
static final class HttpRouteHandler
        implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>,
                    Predicate<HttpServerRequest> {

    final Predicate<? super HttpServerRequest>          condition;
    final BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>>
                                                        handler;
    final Function<? super String, Map<String, String>> resolver;

    HttpRouteHandler(Predicate<? super HttpServerRequest> condition,
            BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler,
            @Nullable Function<? super String, Map<String, String>> resolver) {
        this.condition = Objects.requireNonNull(condition, "condition");
        this.handler = Objects.requireNonNull(handler, "handler");
        this.resolver = resolver;
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request,
            HttpServerResponse response) {
        return handler.apply(request.paramsResolver(resolver), response);
    }

    @Override
    public boolean test(HttpServerRequest o) {
        return condition.test(o);
    }
}
```
Here, we may need to resolve parameters in requests. So, this class provides us a interface which is `Function<? super String, Map<String, String>>`. For `condition` and `resolver`, you can follow what we said in the above sections.
At this point, `HttpRouteHandler` will play the role of the request validator and the request handler. At this point, we'll combine them together by a series of logics to become a whole process. Thus, we can use Proxy Pattern to achieve this here.

In` DefaultHttpServerRoutes`, we use a list to manage a bunch of `HttpRouteHandler` instances. When we use `reactor.netty.http.server.HttpServer#handle`, we'll only see the implementation of `BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>`. Thus, all of logic handling should be integrated into the apply() method. Thus, we'll get the following `reactor.netty.http.server.DefaultHttpServerRoutes`:

```java
//reactor.netty.http.server.DefaultHttpServerRoutes
final class DefaultHttpServerRoutes implements HttpServerRoutes {


	private final CopyOnWriteArrayList<HttpRouteHandler> handlers =
			new CopyOnWriteArrayList<>();
	...
	@Override
	public HttpServerRoutes route(Predicate<? super HttpServerRequest> condition,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		Objects.requireNonNull(condition, "condition");
		Objects.requireNonNull(handler, "handler");

		if (condition instanceof HttpPredicate) {
			handlers.add(new HttpRouteHandler(condition,
					handler,
					(HttpPredicate) condition));
		}
		else {
			handlers.add(new HttpRouteHandler(condition, handler, null));
		}
		return this;
	}

	@Override
	public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
		final Iterator<HttpRouteHandler> iterator = handlers.iterator();
		HttpRouteHandler cursor;

		try {
			while (iterator.hasNext()) {
				cursor = iterator.next();
				if (cursor.test(request)) {
					return cursor.apply(request, response);
				}
			}
		}
		catch (Throwable t) {
			Exceptions.throwIfJvmFatal(t);
			return Mono.error(t); //500
		}

		return response.sendNotFound();
	}
    ...
}
```
In the `route()` method, it just creates a new instance of `HttpRouteHandler` and put this handler into list. Then in the implementation of `apply()`, we can combine all logic into this method. So that, we can design a route method in `reactor.netty.http.server.HttpServer` to provide a `SPI` . Then we should put the whole handling process into this method. It means we need to get an instance of `HttpServerRoutes`, then invoke its `route()` method to build the matching rules. For how to build the matching rules, please check the above sections. Finally, we'll get `HttpServerRoutes`. It plays the role of `BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>`. Then we can use this as the argument of `HttpServer#handle`.

Furthermore，we need to be mindful here. For the `apply()` method implemented in `DefaultHttpServerRoutes`, we can see that once the request match the rule, we will start to handle the request and return the result. We don't need to do a matchup any more. That means when we receive a new request, we will only invoke the first matched handler.


```java
//reactor.netty.http.server.HttpServer#route
public final HttpServer route(Consumer<? super HttpServerRoutes> routesBuilder) {
    Objects.requireNonNull(routesBuilder, "routeBuilder");
    HttpServerRoutes routes = HttpServerRoutes.newRoutes();
    routesBuilder.accept(routes);
    return handle(routes);
}
```


So that, we can apply the above design to the following `Demo`.

```java
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

public class Application {

    public static void main(String[] args) {
        DisposableServer server =
                HttpServer.create()
                          .route(routes ->
                              routes.get("/hello",        <1>
                                         (request, response) -> response.sendString(Mono.just("Hello World!")))
                                    .post("/echo",        <2>
                                         (request, response) -> response.send(request.receive().retain()))
                                    .get("/path/{param}", <3>
                                         (request, response) -> response.sendString(Mono.just(request.param("param")))))
                          .bindNow();

        server.onDispose()
              .block();
    }
}
```

At` <1>`, when we send out a `GET` request to `access /hello`, then we'll get a string which is `Hello World!`.

At `<2>`，when we send out a `POST` request to `access /echo`, then Server will use the request body as responding content to return.

At `<3>`，when we send out a `GET` request to `access /path/{param}`, then we'll get the value of `param` in requesting URL.

For the use of `SSE`, we can see the following demo. The details of codes will not be explained in detail, please check comments.


```java
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.function.BiFunction;

public class Application {

    public static void main(String[] args) {
        DisposableServer server =
                HttpServer.create()
                          .route(routes -> routes.get("/sse", serveSse()))
                          .bindNow();

        server.onDispose()
              .block();
    }

    /**
     * Prepare SSE response
     * Please check reactor.netty.http.server.HttpServerResponse#sse, then you can know 	 * its "Content-Type" is "text/event-stream".
     * If Publisher emits the element, then we'll flush it.
     */
    private static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> serveSse() {
        Flux<Long> flux = Flux.interval(Duration.ofSeconds(10));
        return (request, response) ->
            response.sse()
                    .send(flux.map(Application::toByteBuf), b -> true);
    }

    /**
     * Transformming elements that will be sent out from Object to ByteBuf
     */
    private static ByteBuf toByteBuf(Object any) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write("data: ".getBytes(Charset.defaultCharset()));
            MAPPER.writeValue(out, any);
            out.write("\n\n".getBytes(Charset.defaultCharset()));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ByteBufAllocator.DEFAULT
                               .buffer()
                               .writeBytes(out.toByteArray());
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
}
```