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

import akka.actor.ActorSystem
import akka.stream.testkit.scaladsl.TestSink
import io.opentelemetry.common.Labels
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.data.MetricData.{ Descriptor, LongPoint }
import io.opentelemetry.sdk.resources.Resource
import utest._

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

object StreamMetricExporterTest extends TestSuite {

  implicit lazy val system: ActorSystem =
    ActorSystem("metrics-reporter-tests")

  override def utestAfterAll(): Unit =
    system.terminate()

  implicit lazy val ec: ExecutionContext =
    system.dispatcher

  val tests: Tests = Tests {
    test("consume and shutdown") {
      val metric = MetricData.create(
        Descriptor.create(
          "name",
          "description",
          "1",
          Descriptor.Type.MONOTONIC_LONG,
          Labels.empty()
        ),
        Resource.getEmpty,
        InstrumentationLibraryInfo.getEmpty,
        java.util.Collections.emptyList()
      )
      val metrics = List(metric).asJava

      val exporter = StreamMetricExporter(1)

      val sourceSubscriber = exporter.source.runWith(TestSink.probe[MetricData])

      exporter.`export`(metrics)

      sourceSubscriber.request(1).expectNext(metric)

      exporter.shutdown()

      sourceSubscriber.request(1).expectComplete()
    }
  }
}
