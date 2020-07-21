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

import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.test.TestSpanData
import io.opentelemetry.trace.{ Span, SpanId, Status, TraceId }
import utest._

object TracingProtobufMarshallingTest extends TestSuite {

  val tests: Tests = Tests {
    test("write traces") {
      val spanData: SpanData = TestSpanData
        .newBuilder()
        .setTraceId(new TraceId(0, 0))
        .setSpanId(new SpanId(0))
        .setName("name")
        .setKind(Span.Kind.INTERNAL)
        .setStartEpochNanos(0)
        .setStatus(Status.ABORTED)
        .setEndEpochNanos(0)
        .setHasEnded(true)
        .build()

      import SpanProtobufMarshalling._
      val span = spanData.toProtobuf.build()
      span.getName ==> "name"
    }
  }
}
