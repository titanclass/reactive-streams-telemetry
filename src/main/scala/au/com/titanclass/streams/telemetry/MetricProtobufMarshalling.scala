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

import io.opentelemetry.common.ReadableKeyValuePairs
import io.opentelemetry.proto.common.v1.StringKeyValue
import io.opentelemetry.proto.metrics.v1.{
  DoubleDataPoint,
  Int64DataPoint,
  Metric,
  MetricDescriptor,
  SummaryDataPoint
}
import io.opentelemetry.sdk.metrics.data.MetricData

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

/**
  * Marshalls metric data to the Open Telemetry protobuf form
  */
object MetricProtobufMarshalling {
  implicit class Marshaller(metricData: MetricData) {
    def toProtobuf: Metric.Builder = {
      val metricDataDescriptor = metricData.getDescriptor
      val doubleDataPoints     = new ArrayBuffer[DoubleDataPoint](metricData.getPoints.size())
      val int64DataPoints      = new ArrayBuffer[Int64DataPoint](metricData.getPoints.size())
      val summaryDataPoints    = new ArrayBuffer[SummaryDataPoint](metricData.getPoints.size())
      metricData.getPoints.forEach {
        case p: MetricData.DoublePoint =>
          val _ = doubleDataPoints += DoubleDataPoint
                .newBuilder()
                .addAllLabels(stringKeyValues(p.getLabels).asJava)
                .setStartTimeUnixNano(p.getStartEpochNanos)
                .setTimeUnixNano(p.getEpochNanos)
                .setValue(p.getValue)
                .build()
        case p: MetricData.LongPoint =>
          val _ = int64DataPoints += Int64DataPoint
                .newBuilder()
                .addAllLabels(stringKeyValues(p.getLabels).asJava)
                .setStartTimeUnixNano(p.getStartEpochNanos)
                .setTimeUnixNano(p.getEpochNanos)
                .setValue(p.getValue)
                .build()
        case p: MetricData.SummaryPoint =>
          val _ = summaryDataPoints += SummaryDataPoint
                .newBuilder()
                .addAllLabels(stringKeyValues(p.getLabels).asJava)
                .setStartTimeUnixNano(p.getStartEpochNanos)
                .setTimeUnixNano(p.getEpochNanos)
                .setCount(p.getCount)
                .addAllPercentileValues(p.getPercentileValues.asScala.map { pv =>
                  SummaryDataPoint.ValueAtPercentile
                    .newBuilder()
                    .setPercentile(pv.getPercentile)
                    .setValue(pv.getValue)
                    .build()
                }.asJava)
                .build()
      }
      Metric
        .newBuilder()
        .setMetricDescriptor(
          MetricDescriptor
            .newBuilder()
            .setDescription(metricDataDescriptor.getDescription)
            .addAllLabels(stringKeyValues(metricDataDescriptor.getConstantLabels).asJava)
            .setName(metricDataDescriptor.getName)
            .setType(metricDataDescriptor.getType match {
              case MetricData.Descriptor.Type.MONOTONIC_DOUBLE =>
                MetricDescriptor.Type.COUNTER_DOUBLE
              case MetricData.Descriptor.Type.MONOTONIC_LONG =>
                MetricDescriptor.Type.COUNTER_INT64
              case MetricData.Descriptor.Type.NON_MONOTONIC_DOUBLE =>
                MetricDescriptor.Type.GAUGE_DOUBLE
              case MetricData.Descriptor.Type.NON_MONOTONIC_LONG =>
                MetricDescriptor.Type.GAUGE_INT64
              case MetricData.Descriptor.Type.SUMMARY =>
                MetricDescriptor.Type.SUMMARY
            })
            .setUnit(metricDataDescriptor.getUnit)
        )
        .addAllDoubleDataPoints(doubleDataPoints.asJava)
        .addAllInt64DataPoints(int64DataPoints.asJava)
        .addAllSummaryDataPoints(summaryDataPoints.asJava)
    }
  }

  private def stringKeyValues(kvs: ReadableKeyValuePairs[String]): ArrayBuffer[StringKeyValue] = {
    val array = new ArrayBuffer[StringKeyValue](kvs.size())
    kvs.forEach { (k, v) =>
      val _ = array += StringKeyValue.newBuilder().setKey(k).setValue(v).build()
    }
    array
  }
}
