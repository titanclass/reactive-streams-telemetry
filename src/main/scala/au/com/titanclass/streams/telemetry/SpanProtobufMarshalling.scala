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

import com.google.protobuf.ByteString
import io.opentelemetry.common.{ AttributeValue, ReadableKeyValuePairs }
import io.opentelemetry.proto.common.v1.AttributeKeyValue
import io.opentelemetry.proto.trace.v1.{ Span => ProtoSpan, Status => ProtoStatus }
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.trace.{ Span, SpanId, Status }

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

/**
  * Marshalls span data to the Open Telemetry protobuf form
  */
object SpanProtobufMarshalling {
  implicit class Marshaller(spanData: SpanData) {
    def toProtobuf: ProtoSpan.Builder =
      ProtoSpan
        .newBuilder()
        .addAllAttributes(attributeKeyValues(spanData.getAttributes).asJava)
        .setDroppedAttributesCount(spanData.getTotalAttributeCount - spanData.getAttributes.size())
        .setDroppedEventsCount(spanData.getTotalRecordedEvents - spanData.getEvents.size())
        .setDroppedLinksCount(spanData.getTotalRecordedLinks - spanData.getLinks.size())
        .setEndTimeUnixNano(spanData.getEndEpochNanos)
        .addAllEvents {
          spanData.getEvents.asScala.map { e =>
            ProtoSpan.Event
              .newBuilder()
              .addAllAttributes(attributeKeyValues(e.getAttributes).asJava)
              .setDroppedAttributesCount(e.getTotalAttributeCount - e.getAttributes.size())
              .setName(e.getName)
              .setTimeUnixNano(e.getEpochNanos)
              .build()
          }.asJava
        }
        .setKind {
          spanData.getKind match {
            case Span.Kind.CLIENT   => ProtoSpan.SpanKind.CLIENT
            case Span.Kind.CONSUMER => ProtoSpan.SpanKind.CONSUMER
            case Span.Kind.INTERNAL => ProtoSpan.SpanKind.INTERNAL
            case Span.Kind.PRODUCER => ProtoSpan.SpanKind.PRODUCER
            case Span.Kind.SERVER   => ProtoSpan.SpanKind.SERVER
          }
        }
        .addAllLinks {
          spanData.getLinks.asScala.map { e =>
            ProtoSpan.Link
              .newBuilder()
              .addAllAttributes(attributeKeyValues(e.getAttributes).asJava)
              .setDroppedAttributesCount(e.getTotalAttributeCount - e.getAttributes.size())
              .build()
          }.asJava
        }
        .setName(spanData.getName)
        .setParentSpanId(spanId(spanData.getParentSpanId))
        .setSpanId(spanId(spanData.getSpanId))
        .setStartTimeUnixNano(spanData.getStartEpochNanos)
        .setStatus {
          val status = spanData.getStatus match {
            case Status.ABORTED =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.Aborted)
            case Status.ALREADY_EXISTS =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.AlreadyExists)
            case Status.CANCELLED =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.Cancelled)
            case Status.DATA_LOSS =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.DataLoss)
            case Status.DEADLINE_EXCEEDED =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.DeadlineExceeded)
            case Status.FAILED_PRECONDITION =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.FailedPrecondition)
            case Status.INTERNAL =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.InternalError)
            case Status.INVALID_ARGUMENT =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.InvalidArgument)
            case Status.NOT_FOUND =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.NotFound)
            case Status.OK =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.Ok)
            case Status.OUT_OF_RANGE =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.OutOfRange)
            case Status.PERMISSION_DENIED =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.PermissionDenied)
            case Status.RESOURCE_EXHAUSTED =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.ResourceExhausted)
            case Status.UNAUTHENTICATED =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.Unauthenticated)
            case Status.UNAVAILABLE =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.Unavailable)
            case Status.UNIMPLEMENTED =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.Unimplemented)
            case Status.UNKNOWN =>
              ProtoStatus.newBuilder().setCode(ProtoStatus.StatusCode.UnknownError)
          }
          val desc = spanData.getStatus.getDescription
          if (desc != null) status.setMessage(spanData.getStatus.getDescription) else status
        }
        .setTraceId {
          val bytes = Array.ofDim[Byte](16)
          spanData.getTraceId.copyBytesTo(bytes, 0)
          ByteString.copyFrom(bytes)
        }
        .setTraceState {
          spanData.getTraceState.getEntries.asScala
            .map(e => e.getKey + "=" + e.getValue)
            .mkString(",")
        }
  }

  private def attributeKeyValues(
      kvs: ReadableKeyValuePairs[AttributeValue]
  ): ArrayBuffer[AttributeKeyValue] = {
    val array = new ArrayBuffer[AttributeKeyValue](kvs.size())
    kvs.forEach { (k, v) =>
      val b = AttributeKeyValue.newBuilder().setKey(k)

      {
        val _ = v.getType match {
          case AttributeValue.Type.STRING        => b.setStringValue(v.getStringValue)
          case AttributeValue.Type.BOOLEAN       => b.setBoolValue(v.getBooleanValue)
          case AttributeValue.Type.LONG          => b.setIntValue(v.getLongValue)
          case AttributeValue.Type.DOUBLE        => b.setDoubleValue(v.getDoubleValue)
          case AttributeValue.Type.STRING_ARRAY  => // FIXME: When released https://github.com/open-telemetry/opentelemetry-proto/commit/e43e1abc40428a6ee98e3bfd79bec1dfa2ed18cd
          case AttributeValue.Type.BOOLEAN_ARRAY => // FIXME: When released https://github.com/open-telemetry/opentelemetry-proto/commit/e43e1abc40428a6ee98e3bfd79bec1dfa2ed18cd
          case AttributeValue.Type.LONG_ARRAY    => // FIXME: When released https://github.com/open-telemetry/opentelemetry-proto/commit/e43e1abc40428a6ee98e3bfd79bec1dfa2ed18cd
          case AttributeValue.Type.DOUBLE_ARRAY  => // FIXME: When released https://github.com/open-telemetry/opentelemetry-proto/commit/e43e1abc40428a6ee98e3bfd79bec1dfa2ed18cd
        }
      }

      {
        val _ = array += b.build()
      }
    }
    array
  }

  private def spanId(spanId: SpanId): ByteString = {
    val bytes = Array.ofDim[Byte](8)
    spanId.copyBytesTo(bytes, 0)
    ByteString.copyFrom(bytes)
  }
}
