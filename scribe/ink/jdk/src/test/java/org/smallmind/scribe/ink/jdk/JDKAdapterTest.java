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

import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Record;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JDKAdapterTest {

  private Record<LogRecord> buildRecord (String message) {

    return new JDKRecordSubverter("jdk.adapter.logger", Level.INFO, null, null, message).getRecord();
  }

  public void testFilterAdapterDelegatesPositiveDecision () {

    Filter nativeFilter = (record) -> true;
    JDKFilterAdapter adapter = new JDKFilterAdapter(nativeFilter);

    Assert.assertSame(adapter.getNativeFilter(), nativeFilter);
    Assert.assertTrue(adapter.willLog(buildRecord("anything")));
  }

  public void testFilterAdapterDelegatesNegativeDecision () {

    Filter nativeFilter = (record) -> false;
    JDKFilterAdapter adapter = new JDKFilterAdapter(nativeFilter);

    Assert.assertFalse(adapter.willLog(buildRecord("anything")));
  }

  public void testFilterAdapterPassesNativeRecordThrough () {

    Filter nativeFilter = (record) -> "keep".equals(record.getMessage());
    JDKFilterAdapter adapter = new JDKFilterAdapter(nativeFilter);

    Assert.assertTrue(adapter.willLog(buildRecord("keep")));
    Assert.assertFalse(adapter.willLog(buildRecord("drop")));
  }

  public void testFilterAdapterEqualityUnwrapsNativeFilter () {

    Filter nativeFilter = (record) -> true;

    Assert.assertEquals(new JDKFilterAdapter(nativeFilter), new JDKFilterAdapter(nativeFilter));
    Assert.assertEquals(new JDKFilterAdapter(nativeFilter).hashCode(), nativeFilter.hashCode());
  }

  public void testFormatterAdapterConcatenatesBody () {

    Formatter nativeFormatter = new Formatter() {

      @Override
      public String format (LogRecord record) {

        return "[" + record.getMessage() + "]";
      }
    };
    JDKFormatterAdapter adapter = new JDKFormatterAdapter(nativeFormatter);

    Assert.assertSame(adapter.getNativeFormatter(), nativeFormatter);
    Assert.assertEquals(adapter.format(buildRecord("hello")), "[hello]");
  }

  public void testFormatterAdapterPrependsHeadAndAppendsTail () {

    Formatter nativeFormatter = new Formatter() {

      @Override
      public String getHead (java.util.logging.Handler handler) {

        return "HEAD-";
      }

      @Override
      public String format (LogRecord record) {

        return record.getMessage();
      }

      @Override
      public String getTail (java.util.logging.Handler handler) {

        return "-TAIL";
      }
    };
    JDKFormatterAdapter adapter = new JDKFormatterAdapter(nativeFormatter);

    Assert.assertEquals(adapter.format(buildRecord("body")), "HEAD-body-TAIL");
  }
}
