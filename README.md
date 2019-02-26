# reactive-streams-telemetry #

Welcome to reactive-streams-telemetry!

The goal of this [Scala](https://www.scala-lang.org/) library is to provide an [Reactive Streams](http://www.reactive-streams.org/) interface to 
an application instance's working set of metrics and traces, thereby providing control over resource usage. 

Metrics and traces are presented using streams where elements pertain to a [metric](https://metrics.dropwizard.io/3.1.0/manual/core/)
and [span](https://opentracing.io/docs/overview/spans/) respectively. 
Applications can then consume these streams and present them 
however they wish e.g. [Akka HTTP](https://doc.akka.io/docs/akka-http/current/) can be used to serve a snapshot of metrics and traces as JSON
with an HTTP `GET` route. Another example could be where metrics and traces are published
over UDP to your favorite collection engine. 

[Akka Streams](https://doc.akka.io/docs/akka/2.5/stream/)
is used as the Reactive Streams interface and implementation 
with reporting for [Drop Wizard Metrics](https://metrics.dropwizard.io/4.0.0/) and
[Open Tracing](https://opentracing.io/) via [Jaeger Tracing](https://www.jaegertracing.io/). 
We also provide a JSON encoding as a convenience and use [spray-json](https://github.com/spray/spray-json) 
for this purpose.

Other than the libraries declared above, there are no additional dependencies.

## Teaser

Serve up telemetry given an [Alpakka Unix Domain Socket](https://doc.akka.io/docs/alpakka/current/unix-domain-socket.html) 
and the establishment of the `metrics` and `traces` sources (described following this):

```scala
val flow = Flow[ByteString]
  .dropWhile(_ => true) // Don't care about input to the socket here
  .merge(metrics.map { snapshot =>
    import MetricsJsonProtocol._
    JsObject("metrics" -> snapshot.toJson).compactPrint
  })
  .merge(traces.map { span =>
    import TracingJsonProtocol._
    JsObject("traces" -> span.toJson).compactPrint
  })
  .collect { case s: String => ByteString(s) }
UnixDomainSocket().bindAndHandle(flow, new File("/var/run/mysocket.sock"))
```

## Metrics setup

```scala
import com.codahale.metrics._
import au.com.titanclass.streams.telemetry._
import java.util.concurrent.TimeUnit

val metricRegistry = new MetricRegistry()

val reporter = new MetricsReporter(
  metricRegistry,
  MetricFilter.ALL,
  TimeUnit.HOURS,
  TimeUnit.MILLISECONDS,
  None
)
```

## Tracing setup

```scala
import io.jaegertracing.Tracer
import au.com.titanclass.streams.telemetry._

val reporter = new TracingReporter(1)

val tracer = new Tracer.Builder("my-tracing-service")
  .withReporter(reporter)
  .build()
```

## JSON serialization

To use the JSON serialization you import `MetricsJsonProtocol` or `TracingJsonProtocol` for metrics
and tracing respectively. Here's an example of how to serialize `Span` objects to JSON:

```scala
import au.com.titanclass.streams.telemetry.{TracingJsonProtocol, TracingReporter}
import io.jaegertracing.Tracer
import io.jaegertracing.samplers.ConstSampler
import akka.stream.scaladsl.Sink

val reporter = new TracingReporter(1)

val sampler = new ConstSampler(true)

val tracer = new Tracer.Builder("tracing-reporter-tests")
  .withReporter(reporter)
  .withSampler(sampler)
  .build()

val scope = tracer.buildSpan("some-span").startActive(true)
scope.span().log(0, "hello-world")
scope.close()

import TracingJsonProtocol._

reporter.source
  .runWith(Sink.head)
  .map (_.toJson)
```

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original author. Along with
any pull requests, please state that the contribution is your original work and that you license
the work to the project under the project's open source license. Whether or not you state this
explicitly, by submitting any copyrighted material via pull request, email, or other means you
agree to license the material under the project's open source license and warrant that you have the
legal authority to do so.

## License ##

This code is open source software licensed under the
[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

(c) Copyright Titan Class Pty Ltd, 2019