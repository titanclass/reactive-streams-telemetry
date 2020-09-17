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
import io.opentelemetry.sdk.metrics.`export`.MetricExporter
import io.opentelemetry.sdk.metrics.data.MetricData

import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success }

object StreamMetricExporter {
  def apply(bufferSize: Int)(implicit mat: Materializer): StreamMetricExporter =
    new StreamMetricExporter(bufferSize)
}

/**
  * Provides a source of metrics
  */
class StreamMetricExporter(bufferSize: Int)(implicit mat: Materializer) extends MetricExporter {

  private val (metricQueue, metricSource) = Source
    .queue[MetricData](bufferSize, OverflowStrategy.dropHead.withLogLevel(Attributes.LogLevels.Off))
    .toMat(BroadcastHub.sink(1))(Keep.both)
    .run()

  /**
    * Returns a source of metrics
    */
  def source: Source[MetricData, NotUsed] =
    metricSource

  override def `export`(metrics: util.Collection[MetricData]): CompletableResultCode = {
    val resultCode = new CompletableResultCode()
    import mat.executionContext
    Future.sequence(metrics.asScala.map(x => metricQueue.offer(x)).toList).onComplete {
      case Success(_) => resultCode.succeed()
      case Failure(_) => resultCode.fail()
    }
    resultCode
  }

  override def flush(): CompletableResultCode =
    CompletableResultCode.ofSuccess()

  override def shutdown(): Unit =
    metricQueue.complete()
}
