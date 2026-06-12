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
package org.smallmind.scribe.ink.indigenous;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.Parameters;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class IndigenousLoggerAdapterTest {

  @AfterMethod
  public void tearDown () {

    Parameters.getInstance().clear();
  }

  public void testLevelThresholdSuppressesBelowAndPassesAtOrAbove () {

    CapturingAppender appender = new CapturingAppender();
    IndigenousLoggerAdapter adapter = new IndigenousLoggerAdapter("threshold");

    adapter.addAppender(appender);
    adapter.setLevel(Level.INFO);

    adapter.logMessage(Level.DEBUG, null, "below");
    Assert.assertEquals(appender.size(), 0);

    adapter.logMessage(Level.WARN, null, "above");
    Assert.assertEquals(appender.size(), 1);
    Assert.assertEquals(appender.getOnlyRecord().getMessage(), "above");
  }

  public void testLazySupplierNotInvokedBelowThreshold () {

    CapturingAppender appender = new CapturingAppender();
    IndigenousLoggerAdapter adapter = new IndigenousLoggerAdapter("lazy");
    AtomicInteger count = new AtomicInteger(0);
    Supplier<String> supplier = () -> {
      count.incrementAndGet();

      return "lazy message";
    };

    adapter.addAppender(appender);
    adapter.setLevel(Level.INFO);

    adapter.logMessage(Level.DEBUG, null, supplier);
    Assert.assertEquals(count.get(), 0);
    Assert.assertEquals(appender.size(), 0);

    adapter.logMessage(Level.WARN, null, supplier);
    Assert.assertEquals(count.get(), 1);
    Assert.assertEquals(appender.size(), 1);
    Assert.assertEquals(appender.getOnlyRecord().getMessage(), "lazy message");
  }

  public void testOffLevelSuppressesEverything () {

    CapturingAppender appender = new CapturingAppender();
    IndigenousLoggerAdapter adapter = new IndigenousLoggerAdapter("off");

    adapter.addAppender(appender);
    adapter.setLevel(Level.OFF);

    adapter.logMessage(Level.FATAL, null, "fatal");
    adapter.logMessage(Level.INFO, null, "info");
    adapter.logMessage(Level.OFF, null, "off");

    Assert.assertEquals(appender.size(), 0);
  }

  public void testPrintfFormatting () {

    CapturingAppender appender = new CapturingAppender();
    IndigenousLoggerAdapter adapter = new IndigenousLoggerAdapter("printf");

    adapter.addAppender(appender);
    adapter.setLevel(Level.INFO);

    adapter.logMessage(Level.INFO, null, "x %s %d", "y", 5);

    Assert.assertEquals(appender.size(), 1);
    Assert.assertEquals(appender.getOnlyRecord().getMessage(), "x y 5");
  }

  public void testFanOutToMultipleAppenders () {

    CapturingAppender first = new CapturingAppender();
    CapturingAppender second = new CapturingAppender();
    IndigenousLoggerAdapter adapter = new IndigenousLoggerAdapter("fanout");

    adapter.addAppender(first);
    adapter.addAppender(second);
    adapter.setLevel(Level.INFO);

    adapter.logMessage(Level.INFO, null, "broadcast");

    Assert.assertEquals(first.size(), 1);
    Assert.assertEquals(second.size(), 1);
    Assert.assertEquals(first.getOnlyRecord().getMessage(), "broadcast");
    Assert.assertEquals(second.getOnlyRecord().getMessage(), "broadcast");
  }

  public void testFilterVetoBlocksPublishing () {

    CapturingAppender appender = new CapturingAppender();
    IndigenousLoggerAdapter adapter = new IndigenousLoggerAdapter("veto");

    adapter.addAppender(appender);
    adapter.addFilter(record -> false);
    adapter.setLevel(Level.INFO);

    adapter.logMessage(Level.INFO, null, "blocked");

    Assert.assertEquals(appender.size(), 0);
  }

  public void testEnhancerRunsExactlyOnceBeforePublish () {

    CapturingAppender appender = new CapturingAppender();
    IndigenousLoggerAdapter adapter = new IndigenousLoggerAdapter("enhance");
    AtomicInteger count = new AtomicInteger(0);

    adapter.addAppender(appender);
    adapter.addEnhancer(record -> count.incrementAndGet());
    adapter.setLevel(Level.INFO);

    adapter.logMessage(Level.INFO, null, "enhanced");

    Assert.assertEquals(count.get(), 1);
    Assert.assertEquals(appender.size(), 1);
  }

  public void testParametersAttachedToRecord () {

    CapturingAppender appender = new CapturingAppender();
    IndigenousLoggerAdapter adapter = new IndigenousLoggerAdapter("params");

    adapter.addAppender(appender);
    adapter.setLevel(Level.INFO);

    Parameters.getInstance().clear();
    Parameters.getInstance().put("k", "v");

    adapter.logMessage(Level.INFO, null, "withparams");

    Assert.assertEquals(appender.size(), 1);

    Parameter[] parameters = appender.getOnlyRecord().getParameters();
    boolean found = false;

    for (Parameter parameter : parameters) {
      if ("k".equals(parameter.getKey())) {
        found = true;
        Assert.assertEquals(parameter.getValue(), "v");
      }
    }

    Assert.assertTrue(found, "expected parameter 'k' to be attached to the record");
  }

  public void testRecordFieldsArePopulated () {

    CapturingAppender appender = new CapturingAppender();
    IndigenousLoggerAdapter adapter = new IndigenousLoggerAdapter("fields");
    RuntimeException thrown = new RuntimeException("boom");

    adapter.addAppender(appender);
    adapter.setLevel(Level.INFO);

    adapter.logMessage(Level.WARN, thrown, "field %s", "check");

    Assert.assertEquals(appender.size(), 1);

    Record<?> record = appender.getOnlyRecord();

    Assert.assertEquals(record.getLevel(), Level.WARN);
    Assert.assertEquals(record.getLoggerName(), "fields");
    Assert.assertSame(record.getThrown(), thrown);
    Assert.assertEquals(record.getMessage(), "field check");
    Assert.assertTrue(record.getMillis() > 0, "millis should be populated");
    Assert.assertTrue(record.getSequenceNumber() > 0, "sequence number should be populated");
  }
}
