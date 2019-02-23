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
import java.time.Instant
import java.time.temporal.ChronoField

private[impl] object Time {

  /**
    * Return the current time at microsecond resolution
    */
  def currentTimeMicroseconds: Long = {
    val now = Instant.now()
    (now.getEpochSecond * 1000) + now.getLong(ChronoField.MICRO_OF_SECOND)
  }
}
