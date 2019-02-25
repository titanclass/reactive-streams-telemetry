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

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.stream.{ ActorMaterializer, Materializer }
import io.jaegertracing.Tracer
import io.jaegertracing.samplers.ConstSampler
import spray.json._
import utest._

import scala.concurrent.ExecutionContext

object TracingJsonProtocolTest extends TestSuite {

  implicit lazy val system: ActorSystem =
    ActorSystem("tracing-reporter-tests")

  override def utestAfterAll(): Unit =
    system.terminate()

  implicit lazy val mat: Materializer =
    ActorMaterializer()

  implicit lazy val ec: ExecutionContext =
    mat.executionContext

  val tests = Tests {
    'writeTracing - {

      val reporter = new TracingReporter(1)

      val sampler = new ConstSampler(true)

      val tracer = new Tracer.Builder("tracing-reporter-tests")
        .withReporter(reporter)
        .withSampler(sampler)
        .build()

      val scope = tracer.buildSpan("some-span").startActive(true)
      scope.span().log(0, "hello-world")
      scope.close()

      import TracingJsonProtocol._

      reporter.source
        .runWith(Sink.head)
        .map { s =>
          val json = s.toJson
          val re =
            """\{"baggage":\[\],"duration":.*,"logs":\[\{"fields":\{\},"message":"hello-world","time":0}],"operationName":"some-span","references":\[\],"spanId":.*,"start":.*,"tags":\{"sampler.type":"const","sampler.param":"true"\},"traceId":.*\}""".r
          assertMatch(re.findFirstIn(json.compactPrint)) {
            case Some(_) =>
          }
        }
    }

  }
}
