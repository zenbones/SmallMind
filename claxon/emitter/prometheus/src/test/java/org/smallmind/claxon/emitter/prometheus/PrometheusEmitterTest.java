/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.claxon.emitter.prometheus;

import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PrometheusEmitterTest {

  public void testEmitWithoutDataReturnsOnlyEofSentinel () {

    Assert.assertEquals(new PrometheusEmitter().emit(), "# EOF\n");
  }

  public void testEmitRendersUntaggedSample () {

    PrometheusEmitter emitter = new PrometheusEmitter();

    emitter.record("requests", null, new Quantity[] {new Quantity("count", 5.0)});

    Assert.assertEquals(emitter.emit(), "requests:count 5.0\n# EOF\n");
  }

  public void testEmitRendersTaggedSampleWithLabels () {

    PrometheusEmitter emitter = new PrometheusEmitter();

    emitter.record(
      "requests",
      new Tag[] {new Tag("env", "prod")},
      new Quantity[] {new Quantity("count", 5.0)});

    Assert.assertEquals(emitter.emit(), "requests:count{env=\"prod\"} 5.0\n# EOF\n");
  }

  public void testEmitClearsBufferOnSecondCall () {

    PrometheusEmitter emitter = new PrometheusEmitter();

    emitter.record("requests", null, new Quantity[] {new Quantity("count", 5.0)});
    emitter.emit();

    Assert.assertEquals(emitter.emit(), "# EOF\n");
  }

  public void testLatestValueWinsForSameKeyBetweenEmissions () {

    PrometheusEmitter emitter = new PrometheusEmitter();

    emitter.record("requests", null, new Quantity[] {new Quantity("count", 1.0)});
    emitter.record("requests", null, new Quantity[] {new Quantity("count", 99.0)});

    Assert.assertEquals(emitter.emit(), "requests:count 99.0\n# EOF\n");
  }

  public void testEmitFormatsSpecialDoubleValues () {

    PrometheusEmitter emitter = new PrometheusEmitter();

    emitter.record("a", null, new Quantity[] {new Quantity("z", 0.0)});
    emitter.record("b", null, new Quantity[] {new Quantity("z", Double.POSITIVE_INFINITY)});
    emitter.record("c", null, new Quantity[] {new Quantity("z", Double.NEGATIVE_INFINITY)});
    emitter.record("d", null, new Quantity[] {new Quantity("z", Double.NaN)});

    String output = emitter.emit();

    Assert.assertTrue(output.contains("a:z 0\n"));
    Assert.assertTrue(output.contains("b:z +Inf\n"));
    Assert.assertTrue(output.contains("c:z -Inf\n"));
    Assert.assertTrue(output.contains("d:z NaN\n"));
  }

  public void testNameManglingForCamelCase () {

    PrometheusEmitter emitter = new PrometheusEmitter();

    emitter.record("httpRequests", null, new Quantity[] {new Quantity("rateOfChange", 1.0)});

    Assert.assertEquals(emitter.emit(), "http_requests:rate_of_change 1.0\n# EOF\n");
  }

  public void testNameManglingLowercasesConsecutiveUppercaseWithoutInsertingUnderscores () {

    PrometheusEmitter emitter = new PrometheusEmitter();

    emitter.record("URLPath", null, new Quantity[] {new Quantity("HTTPStatus", 1.0)});

    Assert.assertEquals(emitter.emit(), "urlpath:httpstatus 1.0\n# EOF\n");
  }

  public void testNameManglingForLeadingDigit () {

    PrometheusEmitter emitter = new PrometheusEmitter();

    emitter.record("2xxResponses", null, new Quantity[] {new Quantity("count", 4.0)});

    Assert.assertEquals(emitter.emit(), "_2xx_responses:count 4.0\n# EOF\n");
  }

  public void testNameManglingReplacesUnsupportedCharacters () {

    PrometheusEmitter emitter = new PrometheusEmitter();

    emitter.record("foo.bar-baz", null, new Quantity[] {new Quantity("c", 1.0)});

    Assert.assertEquals(emitter.emit(), "foo_bar_baz:c 1.0\n# EOF\n");
  }

  public void testTagValueIsNotMangledButLabelKeyIs () {

    PrometheusEmitter emitter = new PrometheusEmitter();

    emitter.record(
      "m",
      new Tag[] {new Tag("us-east-1", "value-with-dashes")},
      new Quantity[] {new Quantity("c", 1.0)});

    Assert.assertEquals(emitter.emit(), "m:c{us_east_1=\"value-with-dashes\"} 1.0\n# EOF\n");
  }
}
