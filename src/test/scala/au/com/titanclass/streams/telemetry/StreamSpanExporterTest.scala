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

import akka.actor.ActorSystem
import akka.stream.testkit.scaladsl.TestSink
import io.opentelemetry.common.{ Attributes, ReadableAttributes }
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.trace.{ Span, SpanId, Status, TraceFlags, TraceId, TraceState }
import utest._

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

object StreamSpanExporterTest extends TestSuite {

  implicit lazy val system: ActorSystem =
    ActorSystem("tracing-reporter-tests")

  override def utestAfterAll(): Unit =
    system.terminate()

  implicit lazy val ec: ExecutionContext =
    system.dispatcher

  val tests: Tests = Tests {
    test("consume and shutdown") {
      val spanData = new SpanData() {
        override def getTraceId: TraceId =
          new TraceId(0, 0)

        override def getSpanId: SpanId =
          new SpanId(0)

        override def getTraceFlags: TraceFlags =
          TraceFlags.getDefault

        override def getTraceState: TraceState =
          TraceState.getDefault

        override def getParentSpanId: SpanId =
          SpanId.getInvalid

        override def getResource: Resource =
          Resource.getDefault

        override def getInstrumentationLibraryInfo: InstrumentationLibraryInfo =
          InstrumentationLibraryInfo.getEmpty

        override def getName: String =
          "name"

        override def getKind: Span.Kind =
          Span.Kind.INTERNAL

        override def getStartEpochNanos: Long =
          0

        override def getAttributes: ReadableAttributes =
          Attributes.empty()

        override def getEvents: util.List[SpanData.Event] =
          List.empty.asJava

        override def getLinks: util.List[SpanData.Link] =
          List.empty.asJava

        override def getStatus: Status =
          Status.ABORTED

        override def getEndEpochNanos: Long =
          0

        override def getHasRemoteParent: Boolean =
          false

        override def getHasEnded: Boolean =
          true

        override def getTotalRecordedEvents: Int =
          0

        override def getTotalRecordedLinks: Int =
          0

        override def getTotalAttributeCount: Int =
          0
      }

      val spans = List(spanData).asJava

      val exporter = StreamSpanExporter(1)

      val sourceSubscriber = exporter.source.runWith(TestSink.probe[SpanData])

      exporter.`export`(spans)

      sourceSubscriber.request(1).expectNext(spanData)

      exporter.shutdown()

      sourceSubscriber.request(1).expectComplete()
    }
  }
}
