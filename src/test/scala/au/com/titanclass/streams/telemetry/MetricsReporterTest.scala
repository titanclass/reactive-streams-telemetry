/*
 * Copyright 2019 Titan Class Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package au.com.titanclass.streams.telemetry

import java.util
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.codahale.metrics._
import utest._

import scala.concurrent.ExecutionContext

object MetricsReporterTest extends TestSuite {

  implicit lazy val system: ActorSystem =
    ActorSystem("metrics-reporter-tests")

  override def utestAfterAll(): Unit =
    system.terminate()

  implicit lazy val ec: ExecutionContext =
    system.dispatcher

  val tests: Tests = Tests {
    test("test") {
      val metricRegistry = new MetricRegistry()

      val reporter = new MetricsReporter(
        metricRegistry,
        MetricFilter.ALL,
        TimeUnit.SECONDS,
        TimeUnit.SECONDS,
        None
      )

      val gauges     = new util.TreeMap[String, Gauge[_]]()
      val counters   = new util.TreeMap[String, Counter]()
      val histograms = new util.TreeMap[String, Histogram]()
      val meters     = new util.TreeMap[String, Meter]()
      val timers     = new util.TreeMap[String, Timer]()

      val count1 = new Counter()
      count1.inc()
      val count2 = new Counter()
      count2.inc(2)
      counters.put("lora-server.downlink-packets-rx", count1)
      counters.put("lora-server.downlink-packets-tx", count2)

      val meter1 = new Meter()
      meter1.mark(100)
      meters.put("lora-server.valid-data-up", meter1)

      reporter.report(gauges, counters, histograms, meters, timers)

      reporter.source
        .runWith(Sink.head)
        .map(
          assertMatch(_) {
            case (`gauges`, `counters`, `histograms`, `meters`, `timers`) =>
          }
        )
    }
  }
}
