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
package org.smallmind.scribe.ink.jdk;

import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerContext;
import org.smallmind.scribe.pen.Record;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JDKRecordSubverterTest {

  private static class StubLoggerContext implements LoggerContext {

    private final String className;
    private final String methodName;

    private StubLoggerContext (String className, String methodName) {

      this.className = className;
      this.methodName = methodName;
    }

    @Override
    public boolean isFilled () {

      return true;
    }

    @Override
    public void fillIn () {

    }

    @Override
    public String getClassName () {

      return className;
    }

    @Override
    public String getMethodName () {

      return methodName;
    }

    @Override
    public String getFileName () {

      return null;
    }

    @Override
    public boolean isNativeMethod () {

      return false;
    }

    @Override
    public int getLineNumber () {

      return -1;
    }
  }

  public void testRecordExposesScribeLevelAndRawMessage () {

    JDKRecordSubverter subverter = new JDKRecordSubverter("jdk.subverter.logger", Level.WARN, new StubLoggerContext("com.example.Foo", "bar"), null, "raw %s message", "arg");
    Record<LogRecord> record = subverter.getRecord();

    Assert.assertEquals(record.getLevel(), Level.WARN);
    Assert.assertEquals(record.getMessage(), "raw %s message");
    Assert.assertEquals(record.getLoggerName(), "jdk.subverter.logger");
  }

  public void testNativeLogEntryCarriesTranslatedJDKLevel () {

    JDKRecordSubverter subverter = new JDKRecordSubverter("jdk.subverter.logger", Level.WARN, null, null, "message");

    Assert.assertEquals(subverter.getLevel(), java.util.logging.Level.WARNING);
    Assert.assertSame(subverter.getRecord().getNativeLogEntry(), subverter);
  }

  public void testThrowableIsAttachedToRecord () {

    Throwable throwable = new RuntimeException("boom");
    JDKRecordSubverter subverter = new JDKRecordSubverter("jdk.subverter.logger", Level.ERROR, null, throwable, "oops");

    Assert.assertSame(subverter.getRecord().getThrown(), throwable);
    Assert.assertSame(subverter.getThrown(), throwable);
  }

  public void testSourceClassAndMethodComeFromContext () {

    JDKRecordSubverter subverter = new JDKRecordSubverter("jdk.subverter.logger", Level.INFO, new StubLoggerContext("com.example.Source", "doThing"), null, "message");

    Assert.assertEquals(subverter.getSourceClassName(), "com.example.Source");
    Assert.assertEquals(subverter.getSourceMethodName(), "doThing");
  }

  public void testSourceClassAndMethodNullWhenNoContext () {

    JDKRecordSubverter subverter = new JDKRecordSubverter("jdk.subverter.logger", Level.INFO, null, null, "message");

    Assert.assertNull(subverter.getSourceClassName());
    Assert.assertNull(subverter.getSourceMethodName());
  }
}
