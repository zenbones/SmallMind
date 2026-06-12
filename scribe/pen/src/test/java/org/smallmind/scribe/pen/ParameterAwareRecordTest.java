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
package org.smallmind.scribe.pen;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies the parameter storage contract shared by every concrete {@link Record}: an unset record reports a
 * non-null empty array, set parameters round-trip, and clearing (via {@code null} or an empty varargs call)
 * returns to the empty array. Exercised through a minimal concrete subclass since {@link ParameterAwareRecord}
 * is abstract.
 */
@Test(groups = "unit")
public class ParameterAwareRecordTest {

  private ProbeRecord record () {

    return new ProbeRecord();
  }

  public void testDefaultsToNonNullEmptyArray () {

    Parameter[] parameters = record().getParameters();

    Assert.assertNotNull(parameters);
    Assert.assertEquals(parameters.length, 0);
  }

  public void testSetParametersRoundTrip () {

    ProbeRecord record = record();
    Parameter first = new Parameter("requestId", "abc-123");
    Parameter second = new Parameter("tenant", "acme");

    record.setParameters(first, second);

    Parameter[] parameters = record.getParameters();
    Assert.assertEquals(parameters.length, 2);
    Assert.assertSame(parameters[0], first);
    Assert.assertSame(parameters[1], second);
  }

  public void testNullClearsBackToEmptyArray () {

    ProbeRecord record = record();

    record.setParameters(new Parameter("k", "v"));
    record.setParameters((Parameter[])null);

    Assert.assertEquals(record.getParameters().length, 0);
  }

  public void testNoArgumentsClearsBackToEmptyArray () {

    ProbeRecord record = record();

    record.setParameters(new Parameter("k", "v"));
    record.setParameters();

    Assert.assertEquals(record.getParameters().length, 0);
  }

  /**
   * Minimal concrete {@link ParameterAwareRecord} that supplies trivial values for the remaining
   * {@link Record} contract so the inherited parameter logic can be exercised in isolation.
   */
  private static class ProbeRecord extends ParameterAwareRecord<Object> {

    @Override
    public Object getNativeLogEntry () {

      return null;
    }

    @Override
    public String getLoggerName () {

      return "probe.Logger";
    }

    @Override
    public Level getLevel () {

      return Level.INFO;
    }

    @Override
    public Throwable getThrown () {

      return null;
    }

    @Override
    public String getMessage () {

      return null;
    }

    @Override
    public LoggerContext getLoggerContext () {

      return null;
    }

    @Override
    public long getThreadID () {

      return 0;
    }

    @Override
    public String getThreadName () {

      return "probe-thread";
    }

    @Override
    public long getSequenceNumber () {

      return 0;
    }

    @Override
    public long getMillis () {

      return 0;
    }
  }
}
