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

package au.com.titanclass.streams.telemetry.impl
import java.util

import io.opentracing.{ Span, SpanContext }

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object StreamsSpan {
  private final case class SpanImpl(
      tags: Map[String, String],
      logs: List[(Long, Either[String, util.Map[String, _]])],
      operationName: Option[String],
      finishMicros: Option[Long]
  )
}

@SuppressWarnings(
  Array("org.wartremover.warts.Any", "org.wartremover.warts.Var", "org.wartremover.warts.Null")
)
class StreamsSpan(override val context: SpanContext,
                  private[impl] val references: Seq[(String, SpanContext)],
                  private[impl] val startMicros: Long)
    extends Span {
  import StreamsSpan._

  // Has to be a var as OT regards Spans as mutable
  private var current = SpanImpl(Map.empty, List.empty, None, None)

  override def setTag(key: String, value: String): Span = {
    current.synchronized(
      current = current.copy(tags = current.tags + (key -> value))
    )
    this
  }

  override def setTag(key: String, value: Boolean): Span =
    setTag(key, value.toString)

  override def setTag(key: String, value: Number): Span =
    setTag(key, value.toString)

  override def log(fields: util.Map[String, _]): Span =
    log(Time.currentTimeMicroseconds, fields)

  override def log(timestampMicroseconds: Long, fields: util.Map[String, _]): Span = {
    current.synchronized(
      current = current.copy(logs = (timestampMicroseconds -> Right(fields)) +: current.logs)
    )
    this
  }

  override def log(event: String): Span =
    log(Time.currentTimeMicroseconds, event)

  override def log(timestampMicroseconds: Long, event: String): Span = {
    current.synchronized(
      current = current.copy(logs = (timestampMicroseconds -> Left(event)) +: current.logs)
    )
    this
  }

  override def setBaggageItem(key: String, value: String): Span = {
    context match {
      case c: StreamsSpanContext => c.setBaggageItem(key, value)
      case _                     =>
    }
    this
  }

  override def getBaggageItem(key: String): String =
    context match {
      case c: StreamsSpanContext => c.getBaggageItem(key).orNull
      case _                     => null
    }

  override def setOperationName(operationName: String): Span = {
    current.synchronized(
      current = current.copy(operationName = Some(operationName))
    )
    this
  }

  override def finish(): Unit =
    finish(Time.currentTimeMicroseconds)

  override def finish(finishMicros: Long): Unit =
    current.synchronized(
      current = current.copy(finishMicros = Some(finishMicros))
    )
}
