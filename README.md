# reactive-streams-telemetry #

[![maven-central-badge][]][maven-central] [![build-badge][]][build]

[maven-central]:         https://search.maven.org/#search%7Cga%7C1%7Creactive-streams-telemetry
[maven-central-badge]:   https://maven-badges.herokuapp.com/maven-central/au.com.titanclass/reactive-streams-telemetry_2.12/badge.svg
[build]:                 https://circleci.com/gh/titanclass/reactive-streams-telemetry
[build-badge]:           https://circleci.com/gh/titanclass/reactive-streams-telemetry.svg?style=shield

Welcome to reactive-streams-telemetry!

The goal of this [Scala](https://www.scala-lang.org/)/Java library is to provide a [Reactive Streams](http://www.reactive-streams.org/) interface to [Open Telemetry](https://github.com/open-telemetry/opentelemetry-java) metrics and traces so that low memory utilization can be attained.

Metrics and traces are presented using streams where elements pertain to Open Telemetry's [MetricData](https://github.com/open-telemetry/opentelemetry-java/blob/master/sdk/src/main/java/io/opentelemetry/sdk/metrics/data/MetricData.java)
and [SpanData](https://github.com/open-telemetry/opentelemetry-java/blob/master/sdk/src/main/java/io/opentelemetry/sdk/trace/data/SpanData.java) classes respectively. 
Applications can consume these streams and present them 
however they wish e.g. [Akka HTTP](https://doc.akka.io/docs/akka-http/current/) can be used to serve a snapshot of metrics and traces as JSON
with an HTTP `GET` route. Another example could be where metrics and traces are published
over UDP to your favorite collection engine.

[Akka Streams](https://doc.akka.io/docs/akka/2.6/stream/)
is used as the Reactive Streams interface and implementation.

Other than the libraries declared above, there are no additional dependencies.

## Teaser

Serve up the latest telemetry gathered given an [Alpakka Unix Domain Socket](https://doc.akka.io/docs/alpakka/current/unix-domain-socket.html) 
and the establishment of the `metrics` and `traces` sources from their respective exporters:

```scala
import akka.NotUsed
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.scaladsl.{ Flow, Sink, Source }
import au.com.titanclass.streams.telemetry.{ MetricProtobufMarshalling, SpanProtobufMarshalling }
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.trace.data.SpanData
import java.io.File

val metrics: Source[MetricData, NotUsed] = ???
val traces: Source[SpanData, NotUsed] = ???

val source =
  metrics
    .map { metricData =>
      import MetricProtobufMarshalling._
      metricData.toProtobuf.build().toString
    }
    .merge(
      traces
        .map { spanData =>
          import SpanProtobufMarshalling._
          spanData.toProtobuf.build().toString
        }
    )

UnixDomainSocket()
  .bindAndHandle(Flow.fromSinkAndSourceCoupled(Sink.ignore, source),
                 new File("/var/run/mysocket.sock"))
```

The above will just output the string representations of each element of telemetry.

## Download

Builds are published to Maven Central. Please substitute `version` accordingly.

```
"au.com.titanclass" %% "reactive-streams-telemetry" % version
```

## Usage

Please check out the tests for sample usage.

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original author. Along with
any pull requests, please state that the contribution is your original work and that you license
the work to the project under the project's open source license. Whether or not you state this
explicitly, by submitting any copyrighted material via pull request, email, or other means you
agree to license the material under the project's open source license and warrant that you have the
legal authority to do so.

## Publishing ##

You'll need a GPG key to sign as artifacts are published to Sonatype for publishing at Maven Central.
Once you have GPG setup:

```
export GPG_TTY=$(tty)
sbt publishSigned
```

## License ##

This code is open source software licensed under the
[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

(c) Copyright Titan Class Pty Ltd, 2019