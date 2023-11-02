## Why we need Reactive Programming?

Reactive programming, particularly in the context of the Spring WebFlux framework, addresses the limitations of 
traditional synchronous, blocking I/O-based programming models. Here are some reasons why reactive programming is used 
and how it addresses these limitations:

#### Handling Non-Blocking I/O Operations: 
Traditional REST APIs in Spring MVC typically follow a blocking I/O model. When the server receives a request, it might 
block while waiting for a database query, file system read, or an external service call to complete.
Traditional Spring REST APIs built with Spring MVC use a thread-per-request model. Each incoming request ties up a 
thread until it completes, which can limit scalability in high-throughput scenarios. This can lead to resource wastage 
and potential bottlenecks under high load.

Reactive programming, on the other hand, allows the system to handle these operations asynchronously without blocking, 
which leads to better resource utilization and scalability.

#### Concurrency and Scalability: 
With blocking I/O, the number of simultaneous requests a server can handle is directly tied to the number of available 
threads. This can limit scalability and responsiveness, especially in situations with a large number of concurrent 
connections.

Reactive programming leverages non-blocking I/O to handle a large number of concurrent connections without needing to 
allocate a thread per connection. This can improve the system's ability to handle many simultaneous requests without a 
significant increase in resource usage.

#### Resilience and Responsiveness: 
Reactive systems can be more responsive and resilient to failures. By efficiently managing resources and handling I/O 
operations non-blockingly, the system can better cope with high loads and maintain responsiveness even under heavy 
traffic.

#### Back Pressure and Asynchronous Operations: 
Reactive programming models, which WebFlux supports, handle back pressure 
efficiently. When there's a slower downstream system or client, back pressure mechanisms help to regulate the flow of 
data, allowing systems to handle and process data more effectively.

#### Error Handling: 
Reactive programming models often offer more streamlined error handling through constructs like reactive 
streams, making it easier to propagate and manage errors in the asynchronous workflow.


## Limitations of embedded Tomcat with Spring ReST

Thread pool size for embedded tomcat in Spring MVC is 200. Although, we can increase the thread pool size based on the 
need but only to a certain limit. Let's say you want to support 10,000 concurrent users for your Spring ReST APIs but 
that doesn't mean you will create thread pool of size 10,000 threads. The reason is that thread is an expensive resource 
and can easily take up to 1 MB of heap space. So more threads means more memory consumption by thread itself. This 
leaves very less heap space for actually processing the request. This may cause an issue and impact overall performance 
of the system.

## Some improvements to overcome these limitations

We can make database calls or external APIs calls in parallel. For this, we have ``Callbacks`` 
and ``Futures``. 

![img.png](img/img1.png)

### Callbacks
Callbacks are functions or code snippets that are passed as arguments to other functions, allowing those functions to 
execute the provided code once a specific operation completes. In Java, this is commonly achieved using interfaces or 
functional interfaces like ``java.util.function.Consumer`` or ``java.util.function.BiConsumer``. For instance, when an 
asynchronous operation (like a database call or API request) is initiated, a callback can be attached to handle the 
response or result once it's available.

However, the usage of callbacks comes with certain limitations:

#### Callback Hell/Nesting: 
Asynchronous operations might lead to nested callback functions, making the code harder to read and maintain (commonly 
known as "callback hell"). This happens when multiple asynchronous operations depend on the results of each other, 
leading to deeply nested and complex code structures.

#### Error Handling: 
Error handling in callback-based programming can become cumbersome. Asynchronous errors might not be straightforward to 
handle, leading to less readable and more error-prone code.

### Futures (or Promises in some contexts):
Futures (or Promises) represent a placeholder for a result that will be available at some point in the future. They 
provide a way to perform asynchronous operations and retrieve their results later. In Java, this is facilitated through 
classes like ``java.util.concurrent.Future`` or in modern Java versions, the ``java.util.concurrent.CompletableFuture``.

Limitations of Futures:

#### Limited Composition: 
Chaining asynchronous operations with Futures can be verbose and challenging. Handling scenarios where one operation 
depends on the result of another can lead to complex code, similar to callback nesting.

#### Blocking Waits: 
In some cases, calling ``get()`` on a Future to retrieve the result can block, leading to similar issues as with traditional 
blocking I/O.

![img.png](img/img2.png)

While both callbacks and Futures were steps toward handling asynchronous programming in languages like Java, they have 
limitations in managing complex asynchronous workflows, error handling, and composing multiple asynchronous operations.

## Spring WebFlux

Spring WebFlux, as a reactive programming alternative in the Spring framework, addresses these limitations by allowing 
developers to build non-blocking, asynchronous, and event-driven applications. Spring WebFlux, provide a more 
comprehensive solution by using reactive streams, back pressure, and higher-order functions to address these limitations 
more effectively, offering better composition and error handling in asynchronous and non-blocking scenarios.

### Spring WebFlux Event Loop Model

![img.png](img/img3.png)

The consumer can be notified whatever happened at the producer end based on how the consumer subscribes to the producer. 
So, if any such happens at the producer end, the producer will send an event and then consumer will fetch the data. That 
means the flow of data from application to client happens only when an event is published by a producer, which means the 
data flow is driven by an event.

Let's say that client subscribes to a database fetch event about any insert/update/delete operations on a DB table, so 
if any such operation occur at database end, the producer will publish an event, and since the client is a subscriber, 
it will stream the data.

Here we call producer as Publisher and Consumer as Subscriber.

### Backpressure on Data Streams

Sometimes the application may not be able to handle the response which contains the huge data, this eventually causes
the application to be crashed. In such cases, we can apply the backpressure on the flow of data which tells the publisher
to send the response in a serialized manner i.e., loads the data & sends with the response parts by parts. We can also
let the publisher to know how much data to be sent in the response at a time.

### Reactive Stream Specification
Reactive Streams in Spring WebFlux is an implementation of the Reactive Streams specification. It's a set of interfaces 
and protocols that enable the processing of asynchronous data streams with a focus on non-blocking, backpressure-aware 
processing.

#### Publisher Interface

~~~java
public interface Publisher<T> {
    void subscribe(Subscriber<? super T> var1);
}
~~~

#### Subscriber Interface

~~~java
public interface Subscriber<T> {
    void onSubscribe(Subscription var1);

    void onNext(T var1);

    void onError(Throwable var1);

    void onComplete();
}
~~~

#### Subscription Interface

~~~java
public interface Subscription {
    void request(long var1);

    void cancel();
}
~~~

#### Processor Interface

~~~java
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
~~~

### Reactive Stream Workflow

![img.png](img/img4.png)

**Step-1**: Subscriber will invoke subscribe(-) method of Publisher interface by passing the Subscriber instance as an input

**Step-2:** Publisher acknowledges the Subscriber about its subscription by calling onSubscribe(-) method of Subscriber 
interface. It's an internal call.

**Step-3:** Subscriber will invoke request(-) method of Subscription interface to fetch the data from publisher. It passes 
an input n, means no of data items which subscriber wants at a time from the publisher (backpressure)

**Step-4:** Publisher will send a stream of data to the subscriber by invoking onNext(-) method of Subscriber interface. As 
many data items are there, those many times the publisher will fire the onNext event.

**Step-5:** Once all the data items are received by the Subscriber, publisher will invoke onComplete(-) method of Subscriber 
interface to confirm that it is done with its job and the execution is successful. If there exists any error, publisher 
will invoke onError(-) method of Subscriber interface.

