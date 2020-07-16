package au.com.titanclass.streams.telemetry

import io.opentelemetry.proto.metrics.v1.{ Metric, MetricDescriptor }
import io.opentelemetry.sdk.metrics.data.MetricData

/**
  * Encodes metric data to the Open Telemetry protobuf form
  */
object MetricProtobufMarshalling {
  implicit class Marshaller(metricData: MetricData) {
    def toProtobuf: Metric.Builder =
      Metric
        .newBuilder()
        .setMetricDescriptor(
          MetricDescriptor.newBuilder().setName(metricData.getDescriptor.getName)
        )
  }
}
