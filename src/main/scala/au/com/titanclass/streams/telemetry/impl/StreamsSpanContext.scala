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
import java.{ lang, util }

import io.opentracing.SpanContext

import scala.collection.JavaConverters._

object StreamsSpanContext {
  private final case class SpanContextImpl(baggageItems: Map[String, String])

}

class StreamsSpanContext extends SpanContext {

  import StreamsSpanContext._

  private var current = SpanContextImpl(Map.empty)

  override def baggageItems(): lang.Iterable[
    util.Map.Entry[String, String]
  ] =
    current.baggageItems.asJava.entrySet()

  private[impl] def getBaggageItem(key: String): Option[String] =
    current.baggageItems.get(key)

  private[impl] def setBaggageItem(key: String, value: String): Unit =
    current.synchronized(
      current = current.copy(baggageItems = current.baggageItems + (key -> value))
    )
}
