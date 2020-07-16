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

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ Attributes, OverflowStrategy }
import akka.stream.scaladsl.{ BroadcastHub, Keep, Source }
import io.opentelemetry.sdk.trace.`export`.SpanExporter
import io.opentelemetry.sdk.trace.data.SpanData

object StreamSpanExporter {
  def apply(bufferSize: Int)(implicit system: ActorSystem): StreamSpanExporter =
    new StreamSpanExporter(bufferSize)
}

/**
  * Provides a source of trace spans
  */
class StreamSpanExporter(bufferSize: Int)(implicit system: ActorSystem) extends SpanExporter {

  private val (tracerQueue, tracerSource) = Source
    .queue[SpanData](bufferSize, OverflowStrategy.dropHead.withLogLevel(Attributes.LogLevels.Off))
    .toMat(BroadcastHub.sink(1))(Keep.both)
    .run()

  /**
    * Returns a source of spans
    */
  def source: Source[SpanData, NotUsed] =
    tracerSource

  override def `export`(spans: util.Collection[SpanData]): SpanExporter.ResultCode = {
    spans.forEach { metric =>
      val _ = tracerQueue.offer(metric)
    }
    SpanExporter.ResultCode.SUCCESS
  }

  override def flush(): SpanExporter.ResultCode =
    SpanExporter.ResultCode.SUCCESS

  override def shutdown(): Unit =
    tracerQueue.complete()
}
