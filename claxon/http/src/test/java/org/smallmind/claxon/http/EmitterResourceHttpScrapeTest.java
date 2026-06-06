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
package org.smallmind.claxon.http;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jakarta.ws.rs.core.Response;
import org.smallmind.claxon.emitter.prometheus.PrometheusEmitter;
import org.smallmind.claxon.registry.ClaxonConfiguration;
import org.smallmind.claxon.registry.ClaxonRegistry;
import org.smallmind.claxon.registry.InvalidEmitterException;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.UnknownEmitterException;
import org.smallmind.claxon.registry.meter.TallyBuilder;
import org.smallmind.nutsnbolts.time.Stint;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the round-trip from a meter update, through the registry's collection worker,
 * into a bound {@link PrometheusEmitter}, and out through {@link EmitterResource#get(String)}
 * as a Prometheus text exposition response.
 *
 * <p>The HTTP transport layer itself is not stood up; instead the JAX-RS resource method is
 * invoked directly. This verifies that the lookup, type check, and {@code emit()} delegation
 * inside {@code EmitterResource} cooperate with the registry-driven write path on the
 * Prometheus emitter and that the returned body parses as the expected exposition format.
 */
@Test(groups = "unit")
public class EmitterResourceHttpScrapeTest {

  private static final long AWAIT_TIMEOUT_MILLIS = 10000L;

  private ClaxonRegistry registry;
  private PrometheusEmitter emitter;
  private EmitterResource resource;

  private static String mangle (String original) {

    StringBuilder builder = new StringBuilder();
    boolean lastWasLower = false;

    for (int index = 0; index < original.length(); index++) {

      char ch = original.charAt(index);

      if ((ch >= 'a') && (ch <= 'z')) {
        builder.append(ch);
        lastWasLower = true;
      } else if ((ch >= 'A') && (ch <= 'Z')) {
        if (lastWasLower) {
          builder.append('_');
        }
        builder.append(Character.toLowerCase(ch));
        lastWasLower = false;
      } else if ((ch >= '0') && (ch <= '9')) {
        if (index == 0) {
          builder.append('_');
        }
        builder.append(ch);
        lastWasLower = false;
      } else {
        builder.append('_');
        lastWasLower = false;
      }
    }

    return builder.toString();
  }

  @BeforeMethod
  public void setUp () {

    ClaxonConfiguration configuration = new ClaxonConfiguration();

    configuration.setCollectionStint(new Stint(50, TimeUnit.MILLISECONDS));
    configuration.setNamingStrategy(caller -> caller.getName());
    registry = new ClaxonRegistry(configuration);

    emitter = new PrometheusEmitter();
    registry.bind("prometheus", emitter);

    resource = new EmitterResource(registry);
  }

  @AfterMethod
  public void tearDown ()
    throws InterruptedException, TimeoutException {

    registry.stop();
  }

  public void testScrapeReturnsRegistryDrivenReadingInPrometheusFormat ()
    throws InterruptedException, UnknownEmitterException, InvalidEmitterException {

    registry.register(EmitterResourceHttpScrapeTest.class, new TallyBuilder(), new Tag("zone", "alpha")).update(42L);

    String body = awaitNonEmptyScrape("prometheus", AWAIT_TIMEOUT_MILLIS);
    String expectedMetric = mangle(EmitterResourceHttpScrapeTest.class.getName()) + ":count{zone=\"alpha\"} 42.0";

    Assert.assertTrue(body.contains(expectedMetric), "Expected metric line '" + expectedMetric + "' in body:\n" + body);
    Assert.assertTrue(body.endsWith("# EOF\n"), "Prometheus body should terminate with '# EOF\\n', got:\n" + body);
  }

  public void testScrapeDrainsBufferAfterRegistryStops ()
    throws InterruptedException, TimeoutException, UnknownEmitterException, InvalidEmitterException {

    registry.register(EmitterResourceHttpScrapeTest.class, new TallyBuilder()).update(1L);

    String first = awaitNonEmptyScrape("prometheus", AWAIT_TIMEOUT_MILLIS);

    Assert.assertTrue(first.contains(mangle(EmitterResourceHttpScrapeTest.class.getName()) + ":count"), "First scrape should contain the buffered count:\n" + first);

    registry.stop();

    String second = (String)resource.get("prometheus").getEntity();

    Assert.assertEquals(second, "# EOF\n", "A scrape after the worker has stopped should return an empty body terminated by '# EOF'");
  }

  @Test(groups = "integration", expectedExceptions = UnknownEmitterException.class)
  public void testScrapeOfUnknownEmitterFails ()
    throws UnknownEmitterException, InvalidEmitterException {

    resource.get("absent");
  }

  private String awaitNonEmptyScrape (String emitterName, long timeoutMillis)
    throws InterruptedException, UnknownEmitterException, InvalidEmitterException {

    long deadline = System.currentTimeMillis() + timeoutMillis;

    while (System.currentTimeMillis() < deadline) {

      Response response = resource.get(emitterName);

      Assert.assertEquals(response.getStatus(), 200);

      String body = (String)response.getEntity();

      if ((body != null) && !body.equals("# EOF\n")) {

        return body;
      }

      Thread.sleep(25L);
    }

    Assert.fail("Did not see a populated Prometheus scrape body within " + timeoutMillis + " ms");

    return null;
  }
}
