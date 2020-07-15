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
import java.util.concurrent.{ ScheduledExecutorService, TimeUnit }

import akka.NotUsed
import akka.stream.{ Attributes, Materializer, OverflowStrategy }
import akka.stream.scaladsl.{ BroadcastHub, Keep, Source }
import com.codahale.metrics._

object MetricsReporter {
  type MetricsSnapshot = (
      util.SortedMap[String, Gauge[_]],
      util.SortedMap[String, Counter],
      util.SortedMap[String, Histogram],
      util.SortedMap[String, Meter],
      util.SortedMap[String, Timer]
  )
}

/**
  * Provides a source of Dropwizard Metrics snapshots emitted once per the rate and duration.
  */
@SuppressWarnings(Array("org.wartremover.warts.Null"))
class MetricsReporter(
    registry: MetricRegistry,
    filter: MetricFilter,
    rateUnit: TimeUnit,
    durationUnit: TimeUnit,
    executor: Option[ScheduledExecutorService]
)(implicit mat: Materializer)
    extends ScheduledReporter(
      registry,
      "streams-reporter",
      filter,
      rateUnit,
      durationUnit,
      executor.orNull
    ) {

  import MetricsReporter._

  private val (snapshotQueue, snapshotSource) = Source
    .queue[MetricsSnapshot](1, OverflowStrategy.dropHead.withLogLevel(Attributes.LogLevels.Off))
    .toMat(BroadcastHub.sink(1))(Keep.both)
    .run()

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def report(
      gauges: util.SortedMap[String, Gauge[_]],
      counters: util.SortedMap[String, Counter],
      histograms: util.SortedMap[String, Histogram],
      meters: util.SortedMap[String, Meter],
      timers: util.SortedMap[String, Timer]
  ): Unit = {
    val _ = snapshotQueue.offer((gauges, counters, histograms, meters, timers))
  }

  /**
    * Returns a source of metric snapshots
    */
  def source: Source[MetricsSnapshot, NotUsed] =
    snapshotSource
}
