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

import io.jaegertracing.{ Span => JaegerSpan, SpanContext => JaegerSpanContext }
import io.opentracing.Span
import spray.json._

import scala.collection.JavaConverters._

/**
  * JSON serialization for trace spans.
  *
  * Note that only Jaeger Span objects are catered for.
  */
object TracingJsonProtocol extends DefaultJsonProtocol {

  implicit object SpanFormat extends JsonWriter[Span] {
    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    override def write(obj: Span): JsValue =
      (obj, obj.context) match {
        case (js: JaegerSpan, jsctx: JaegerSpanContext) =>
          JsObject(
            "traceId" -> JsNumber(jsctx.getTraceId),
            "spanId"  -> JsNumber(jsctx.getSpanId),
            "references" -> JsArray(
              js.getReferences.asScala
                .map(x => JsObject(x.getType -> JsNumber(x.getSpanContext.getSpanId)))
                .toVector
            ),
            "operationName" -> JsString(js.getOperationName),
            "start"         -> JsNumber(js.getStart),
            "duration"      -> JsNumber(js.getDuration),
            "tags"          -> JsObject(js.getTags.asScala.mapValues(x => JsString(x.toString)).toMap),
            "logs" -> JsArray(
              js.getLogs.asScala
                .map(
                  x =>
                    JsObject(
                      "time" -> JsNumber(x.getTime),
                      "fields" -> JsObject(
                        x.getFields match {
                          case null => Map.empty[String, JsValue]
                          case f    => f.asScala.map(x => x._1 -> JsString(x._2.toString)).toMap
                        }
                      ),
                      "message" -> JsString(x.getMessage)
                  )
                )
                .toVector
            ),
            "baggage" -> JsArray(
              jsctx
                .baggageItems()
                .asScala
                .map(x => JsObject(x.getKey -> JsString(x.getValue)))
                .toVector
            )
          )
        case _ =>
          JsNull
      }
  }
}
