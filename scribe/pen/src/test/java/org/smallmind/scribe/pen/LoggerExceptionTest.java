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
 * Confirms the printf-style message formatting and cause chaining inherited from the nutsnbolts
 * formatted-exception bases, for both the checked {@link LoggerException} and unchecked
 * {@link LoggerRuntimeException}.
 */
@Test(groups = "unit")
public class LoggerExceptionTest {

  public void testFormattedMessageWithoutCause () {

    LoggerException exception = new LoggerException("appender(%s) failed after %d attempts", "file", 3);

    Assert.assertEquals(exception.getMessage(), "appender(file) failed after 3 attempts");
    Assert.assertNull(exception.getCause());
  }

  public void testFormattedMessageWithCause () {

    IllegalStateException cause = new IllegalStateException("root");
    LoggerException exception = new LoggerException(cause, "wrapping %s", "root");

    Assert.assertEquals(exception.getMessage(), "wrapping root");
    Assert.assertSame(exception.getCause(), cause);
  }

  public void testCauseOnlyConstructor () {

    IllegalStateException cause = new IllegalStateException("root");

    Assert.assertSame(new LoggerException(cause).getCause(), cause);
  }

  public void testNoArgConstructorHasNullMessage () {

    Assert.assertNull(new LoggerException().getMessage());
  }

  public void testRuntimeVariantFormatsIdentically () {

    IllegalStateException cause = new IllegalStateException("root");
    LoggerRuntimeException exception = new LoggerRuntimeException(cause, "code %d", 42);

    Assert.assertEquals(exception.getMessage(), "code 42");
    Assert.assertSame(exception.getCause(), cause);
  }
}
