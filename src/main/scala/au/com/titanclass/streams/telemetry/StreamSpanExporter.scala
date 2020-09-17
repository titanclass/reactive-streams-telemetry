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
import akka.stream.{ Attributes, Materializer, OverflowStrategy }
import akka.stream.scaladsl.{ BroadcastHub, Keep, Source }
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.`export`.SpanExporter
import io.opentelemetry.sdk.trace.data.SpanData

import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success }

object StreamSpanExporter {
  def apply(bufferSize: Int)(implicit mat: Materializer): StreamSpanExporter =
    new StreamSpanExporter(bufferSize)
}

/**
  * Provides a source of trace spans
  */
class StreamSpanExporter(bufferSize: Int)(implicit mat: Materializer) extends SpanExporter {

  private val (tracerQueue, tracerSource) = Source
    .queue[SpanData](bufferSize, OverflowStrategy.dropHead.withLogLevel(Attributes.LogLevels.Off))
    .toMat(BroadcastHub.sink(1))(Keep.both)
    .run()

  /**
    * Returns a source of spans
    */
  def source: Source[SpanData, NotUsed] =
    tracerSource

  override def `export`(spans: util.Collection[SpanData]): CompletableResultCode = {
    val resultCode = new CompletableResultCode()
    import mat.executionContext
    Future.sequence(spans.asScala.map(x => tracerQueue.offer(x)).toList).onComplete {
      case Success(_) => resultCode.succeed()
      case Failure(_) => resultCode.fail()
    }
    resultCode
  }

  override def flush(): CompletableResultCode =
    CompletableResultCode.ofSuccess()

  override def shutdown(): CompletableResultCode = {
    val resultCode = new CompletableResultCode()
    tracerQueue.complete()
    import mat.executionContext
    tracerQueue.watchCompletion().foreach(_ => resultCode.succeed())
    resultCode
  }
}
