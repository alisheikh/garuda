/*
 * Copyright (c) 2013-2013 Erik van Oosten
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

package nl.grons.metrics.scala

import com.codahale.metrics.MetricRegistry
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, OneInstancePerTest}

@RunWith(classOf[JUnitRunner])
class MetricBuilderSpec extends FunSpec with MockitoSugar with ShouldMatchers with OneInstancePerTest {

  private val testMetricRegistry = new MetricRegistry()

  trait Instrumented extends InstrumentedBuilder {
    val metricRegistry = testMetricRegistry
  }

  class UnderTest extends Instrumented {
    val timer: Timer = metrics.timer("10ms")
    val gauge: Gauge[Int] = metrics.gauge("the answer")(value)
    val counter: Counter = metrics.counter("1..2..3..4")
    val histogram: Histogram = metrics.histogram("histo")
    val meter: Meter = metrics.meter("meter", "testscope")

    def waitFor100Ms() {
      timer.time {
        Thread.sleep(100L)
      }
    }

    def value = 42

    def incr() {
      counter += 1
    }

    def meterPlusEleven() {
      meter.mark(11)
    }

    def histogramPlusOne() {
      histogram += 1
    }
  }

  describe("Metrics name builder") {
    it("concatenates names with a period as separator") {
      MetricBuilder.metricName(classOf[MetricBuilder], Seq("part1", "part2")) should equal("nl.grons.metrics.scala.MetricBuilder.part1.part2")
    }

    it("skips nulls") {
      MetricBuilder.metricName(classOf[MetricBuilder], Seq("part1", null)) should equal("nl.grons.metrics.scala.MetricBuilder.part1")
    }

    it("supports closures") {
      val foo: String => Class[_] = s => this.getClass
      MetricBuilder.metricName(foo(""), Seq("part1")) should equal("nl.grons.metrics.scala.MetricBuilderSpec.part1")
    }

    it("supports objects") {
      MetricBuilder.metricName(MetricBuilderSpec.ref, Seq("part1")) should equal("nl.grons.metrics.scala.MetricBuilderSpec.part1")
    }

    it("supports nested objects") {
      MetricBuilder.metricName(MetricBuilderSpec.nestedRef, Seq("part1")) should equal("nl.grons.metrics.scala.MetricBuilderSpec.Nested.part1")
    }

    it("supports packages") {
      MetricBuilder.metricName(nl.grons.metrics.scala.ref, Seq("part1")) should equal("nl.grons.metrics.scala.part1")
    }
  }

  describe("Metrics configuration dsl") {
    val underTest = new UnderTest

    it("defines a timer") {
      underTest.waitFor100Ms()
      underTest.timer.min should be >= 100000000L
    }

    it("defines a gauge") {
      underTest.gauge.value should equal(42)
    }

    it("defines a counter") {
      underTest.incr()
      underTest.counter.count should equal(1)
    }

    it("defines a histogram") {
      underTest.histogramPlusOne()
      underTest.histogram.count should equal(1)
      underTest.histogram.min should equal(1)
    }

    it("defines a meter") {
      underTest.meterPlusEleven()
      underTest.meter.count should equal(11)
    }
  }

}

object MetricBuilderSpec {

  object Nested {
    val ref: Class[_] = this.getClass
  }

  private val ref: Class[_] = this.getClass
  private val nestedRef: Class[_] = Nested.ref
}
