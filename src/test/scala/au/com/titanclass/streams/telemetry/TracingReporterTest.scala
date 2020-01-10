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
import io.jaegertracing.internal.{ JaegerSpan, JaegerTracer => Tracer }
import io.jaegertracing.internal.samplers.ConstSampler
import utest._

import scala.concurrent.ExecutionContext

object TracingReporterTest extends TestSuite {

  implicit lazy val system: ActorSystem =
    ActorSystem("tracing-reporter-tests")

  override def utestAfterAll(): Unit =
    system.terminate()

  implicit lazy val mat: Materializer =
    ActorMaterializer()

  implicit lazy val ec: ExecutionContext =
    mat.executionContext

  val tests: Tests = Tests {
    'test - {
      val reporter = new TracingReporter(1)

      val sampler = new ConstSampler(true)

      val tracer = new Tracer.Builder("tracing-reporter-tests")
        .withReporter(reporter)
        .withSampler(sampler)
        .build()

      val _ = tracer.buildSpan("some-span").start().finish()

      reporter.source
        .runWith(Sink.head)
        .map {
          case s: JaegerSpan =>
            s.getOperationName ==> "some-span"
        }
    }
  }
}
