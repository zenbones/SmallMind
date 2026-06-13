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
package org.smallmind.web.json.scaffold.fault;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the simple fault-package exception wrappers: {@link NativeObjectException} (stack-trace text),
 * {@link ObjectInstantiationException} and {@link ResourceInvocationException} (cause preservation), and
 * {@link FormatteJacksonException} (template formatting with the {@code null}-template branch).
 */
@Test(groups = "unit")
public class FaultExceptionsTest {

  public void testNativeObjectExceptionCapturesStackTraceText () {

    NativeObjectException exception = new NativeObjectException(new IllegalStateException("oops"));

    Assert.assertNotNull(exception.getMessage());
    Assert.assertTrue(exception.getMessage().contains("IllegalStateException"), exception.getMessage());
  }

  public void testObjectInstantiationExceptionPreservesCause () {

    ClassNotFoundException cause = new ClassNotFoundException("com.example.Missing");
    ObjectInstantiationException exception = new ObjectInstantiationException(cause);

    Assert.assertEquals(exception.getCause(), cause);
    Assert.assertEquals(exception.getMessage(), "com.example.Missing");
  }

  public void testResourceInvocationExceptionPreservesCause () {

    RuntimeException cause = new RuntimeException("boom");
    ResourceInvocationException exception = new ResourceInvocationException(cause);

    Assert.assertEquals(exception.getCause(), cause);
    Assert.assertEquals(exception.getMessage(), "boom");
  }

  public void testFormatteJacksonExceptionFormatsTemplate () {

    FormatteJacksonException exception = new FormatteJacksonException("bad value(%s) at index(%d)", "x", 3);

    Assert.assertTrue(exception.getMessage().contains("bad value(x) at index(3)"), exception.getMessage());
  }

  public void testFormatteJacksonExceptionNullTemplateProducesNoMessage () {

    FormatteJacksonException exception = new FormatteJacksonException(null, "ignored");

    // The constructor passes a null message to JacksonException, whose getMessage() reports the
    // placeholder "N/A" for a null detail message rather than returning null.
    Assert.assertEquals(exception.getMessage(), "N/A");
  }
}
