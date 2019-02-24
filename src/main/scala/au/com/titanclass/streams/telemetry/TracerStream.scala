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
import au.com.titanclass.streams.telemetry.impl.{ StreamsScopeManager, StreamsSpanBuilder }
import io.opentracing.{ ScopeManager, Span, SpanContext, Tracer }
import io.opentracing.propagation.Format

object TracerStream extends TracerStream

/**
  * Provides an Open Tracing Tracer that emits a stream of events
  */
class TracerStream extends Tracer {
  private val currentScopeManager = new StreamsScopeManager

  override def scopeManager(): ScopeManager =
    currentScopeManager

  override def activeSpan(): Span =
    scopeManager().active().span()

  override def buildSpan(operationName: String): Tracer.SpanBuilder =
    new StreamsSpanBuilder(currentScopeManager)

  override def inject[C](spanContext: SpanContext, format: Format[C], carrier: C): Unit = ???
  override def extract[C](format: Format[C], carrier: C): SpanContext                   = ???
}
