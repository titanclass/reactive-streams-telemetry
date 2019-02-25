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
import com.codahale.metrics._
import spray.json._

import scala.collection.JavaConverters._

/**
  * JSON serialization for Dropwizard metrics.
  *
  * Note that Gauges are expected to hold numeric quantities only
  */
object MetricsJsonProtocol extends DefaultJsonProtocol {

  final class BigDecimalGauge(val value: BigDecimal) extends Gauge[BigDecimal] {
    override def getValue: BigDecimal = value
  }

  implicit object GaugeFormat extends JsonWriter[BigDecimalGauge] {
    override def write(obj: BigDecimalGauge): JsValue =
      JsNumber(obj.getValue)
  }

  implicit object CounterFormat extends JsonWriter[Counter] {
    override def write(obj: Counter): JsValue =
      JsNumber(obj.getCount)
  }

  private def snapshot(snapshot: Snapshot): Map[String, JsNumber] =
    Map[String, JsNumber](
      "percent75"  -> JsNumber(snapshot.get75thPercentile()),
      "percent95"  -> JsNumber(snapshot.get95thPercentile()),
      "percent98"  -> JsNumber(snapshot.get98thPercentile()),
      "percent99"  -> JsNumber(snapshot.get99thPercentile()),
      "percent999" -> JsNumber(snapshot.get999thPercentile()),
      "max"        -> JsNumber(snapshot.getMax),
      "mean"       -> JsNumber(snapshot.getMean),
      "median"     -> JsNumber(snapshot.getMedian),
      "min"        -> JsNumber(snapshot.getMin),
      "stdDev"     -> JsNumber(snapshot.getStdDev)
    )

  implicit object HistogramFormat extends JsonWriter[Histogram] {
    override def write(obj: Histogram): JsValue =
      JsObject(
        Map[String, JsNumber]("count" -> JsNumber(obj.getCount)) ++ snapshot(obj.getSnapshot)
      )
  }

  private def metered(metered: Metered): Map[String, JsNumber] =
    Map[String, JsNumber](
      "count"             -> JsNumber(metered.getCount),
      "fifteenMinuteRate" -> JsNumber(metered.getFifteenMinuteRate),
      "fiveMinuteRate"    -> JsNumber(metered.getFiveMinuteRate),
      "meanRate"          -> JsNumber(metered.getMeanRate),
      "oneMinuteRate"     -> JsNumber(metered.getOneMinuteRate)
    )

  implicit object MeterFormat extends JsonWriter[Meter] {
    override def write(obj: Meter): JsValue =
      JsObject(metered(obj))
  }

  implicit object TimerFormat extends JsonWriter[Timer] {
    override def write(obj: Timer): JsValue =
      JsObject(metered(obj) ++ snapshot(obj.getSnapshot))
  }

  implicit object MetricsSnapshotFormat extends JsonWriter[MetricsReporter.MetricsSnapshot] {
    override def write(obj: MetricsReporter.MetricsSnapshot): JsValue =
      JsObject(
        "gauges" -> JsObject(
          obj._1.asScala
            .mapValues(g => new BigDecimalGauge(BigDecimal(g.getValue.toString)).toJson)
            .toMap
        ),
        "counters"   -> JsObject(obj._2.asScala.mapValues(_.toJson).toMap),
        "histograms" -> JsObject(obj._3.asScala.mapValues(_.toJson).toMap),
        "meters"     -> JsObject(obj._4.asScala.mapValues(_.toJson).toMap),
        "timers"     -> JsObject(obj._5.asScala.mapValues(_.toJson).toMap)
      )
  }
}
