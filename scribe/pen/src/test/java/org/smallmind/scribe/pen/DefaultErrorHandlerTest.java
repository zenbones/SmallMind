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
 * Exercises {@link DefaultErrorHandler}, which builds a FATAL error record through the active
 * {@code LoggingBlueprint} (here the test-only recording blueprint) and publishes it to its backup
 * appender. The {@code process(Record, ...)} overload additionally re-publishes the original record.
 */
@Test(groups = "unit")
public class DefaultErrorHandlerTest {

  public void testLoggerNameProcessPublishesFatalErrorRecord () {

    CapturingAppender backup = new CapturingAppender();
    DefaultErrorHandler handler = new DefaultErrorHandler(backup);
    RuntimeException boom = new RuntimeException("boom");

    handler.process("my.logger", boom, "failed for %s", "ctx");

    Assert.assertEquals(backup.size(), 1);

    Record<?> errorRecord = backup.getRecords().get(0);
    Assert.assertEquals(errorRecord.getLevel(), Level.FATAL);
    Assert.assertEquals(errorRecord.getLoggerName(), "my.logger");
    Assert.assertEquals(errorRecord.getMessage(), "failed for ctx");
    Assert.assertSame(errorRecord.getThrown(), boom);
  }

  public void testRecordProcessRepublishesOriginalRecordAndErrorRecord () {

    CapturingAppender backup = new CapturingAppender();
    DefaultErrorHandler handler = new DefaultErrorHandler(backup);
    RecordFixture original = new RecordFixture().setLoggerName("origin").setMessage("original");

    handler.process(original, new RuntimeException("boom"), "publish failed");

    Assert.assertEquals(backup.size(), 2);
    Assert.assertTrue(backup.getRecords().contains(original));
  }
}
