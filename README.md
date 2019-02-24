# reactive-streams-telemetry #

Welcome to reactive-streams-telemetry!

The goal of this library is to provide an Reactive Streams interface to 
an application instance's working set of metrics and traces. Metrics and
traces are presented using streams where elements pertain to their
primitives. Applications can then consume these streams and present them 
however they wish e.g. Akka HTTP can be used to serve a snapshot of metrics and traces as JSON
with a route using HTTP `GET`. Another example could be where metrics and traces are published
over UDP to your favorite collection engine.
 
[Akka Streams](https://doc.akka.io/docs/akka/2.5/stream/)
is used as the [Reactive Streams](http://www.reactive-streams.org/) interface and implementation 
with reporting for [Drop Wizard Metrics](https://metrics.dropwizard.io/4.0.0/) and
[Open Tracing](https://opentracing.io/) via [Jaeger Tracing](https://www.jaegertracing.io/). 

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