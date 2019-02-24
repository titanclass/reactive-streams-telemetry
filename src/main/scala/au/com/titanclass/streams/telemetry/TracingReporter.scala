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
import akka.NotUsed
import akka.stream.{ KillSwitches, Materializer, OverflowStrategy }
import akka.stream.scaladsl.{ BroadcastHub, Keep, Source }
import io.jaegertracing.Span
import io.jaegertracing.reporters.Reporter

/**
  * Provides a source of Jaeger traces.
  */
class TracingReporter(bufferSize: Int)(implicit mat: Materializer) extends Reporter {

  private val ((tracerQueue, killSwitch), tracerSource) = Source
    .queue[Span](bufferSize, OverflowStrategy.dropHead)
    .viaMat(KillSwitches.single)(Keep.both)
    .toMat(BroadcastHub.sink(1))(Keep.both)
    .run()

  override def report(span: Span): Unit = {
    val _ = tracerQueue.offer(span)
  }

  override def close(): Unit =
    killSwitch.shutdown()

  /**
    * Returns a source of metric snapshots
    */
  def source: Source[Span, NotUsed] =
    tracerSource
}
