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
import io.opentracing.{ References, Scope, Span, SpanContext }
import io.opentracing.Tracer.SpanBuilder

object StreamsSpanBuilder {
  private final case class SpanBuilderImpl(
      references: Seq[(String, SpanContext)],
      ignoreActiveSpan: Boolean,
      tags: Map[String, String],
      startMicros: Option[Long]
  )
}

@SuppressWarnings(Array("org.wartremover.warts.Var"))
class StreamsSpanBuilder(private val scopeManager: StreamsScopeManager) extends SpanBuilder {
  import StreamsSpanBuilder._

  private var current = SpanBuilderImpl(List.empty, ignoreActiveSpan = false, Map.empty, None)

  override def asChildOf(
      parent: SpanContext
  ): SpanBuilder =
    addReference(References.CHILD_OF, parent)

  override def asChildOf(parent: Span): SpanBuilder =
    addReference(References.CHILD_OF, parent.context())

  override def addReference(referenceType: String, referencedContext: SpanContext): SpanBuilder = {
    current.synchronized(
      current =
        current.copy(references = (referenceType -> referencedContext) +: current.references)
    )
    this
  }

  override def ignoreActiveSpan(): SpanBuilder = {
    current.synchronized(
      current = current.copy(ignoreActiveSpan = true)
    )
    this
  }

  override def withTag(key: String, value: String): SpanBuilder = {
    current.synchronized(
      current = current.copy(tags = current.tags + (key -> value))
    )
    this
  }

  override def withTag(key: String, value: Boolean): SpanBuilder =
    withTag(key, value.toString)

  override def withTag(key: String, value: Number): SpanBuilder =
    withTag(key, value.toString)

  override def withStartTimestamp(microseconds: Long): SpanBuilder = {
    current.synchronized(
      current = current.copy(startMicros = Some(microseconds))
    )
    this
  }

  override def startActive(finishSpanOnClose: Boolean): Scope =
    scopeManager.activate(start(), finishSpanOnClose)

  override def startManual(): Span =
    start()

  override def start(): Span =
    current.synchronized {
      // Check if active span should be established as CHILD_OF relationship
      val newReferences =
        if (current.references.isEmpty && !current.ignoreActiveSpan && null != scopeManager
              .active())
          Vector(References.CHILD_OF -> scopeManager.active().span().context())
        else
          current.references

      new StreamsSpan(new StreamsSpanContext(),
                      newReferences,
                      current.startMicros.getOrElse(Time.currentTimeMicroseconds))
    }
}
