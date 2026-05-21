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
package org.smallmind.bayeux.oumuamua.server.api;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class OumuamuaExceptionTest {

  public void testMessageOnlyConstructorProducesThrowable () {

    OumuamuaException ex = new OumuamuaException("error %s", "detail");

    Assert.assertNotNull(ex.getMessage());
    Assert.assertNull(ex.getCause());
  }

  public void testThrowableWrapConstructorPreservesCause () {

    RuntimeException cause = new RuntimeException("root");
    OumuamuaException ex = new OumuamuaException(cause);

    Assert.assertSame(ex.getCause(), cause);
  }

  public void testThrowableAndMessageConstructorPreservesCause () {

    RuntimeException cause = new RuntimeException("root");
    OumuamuaException ex = new OumuamuaException(cause, "wrapper %s", "msg");

    Assert.assertSame(ex.getCause(), cause);
    Assert.assertNotNull(ex.getMessage());
  }

  public void testChannelStateExceptionIsSubclassOfOumuamuaException () {

    ChannelStateException ex = new ChannelStateException("bad state");

    Assert.assertTrue(ex instanceof OumuamuaException);
  }

  public void testInvalidPathExceptionIsSubclassOfOumuamuaException () {

    InvalidPathException ex = new InvalidPathException("bad path %s", "/bad");

    Assert.assertTrue(ex instanceof OumuamuaException);
  }

  @Test(expectedExceptions = ChannelStateException.class)
  public void testChannelStateExceptionCanBeThrown ()
    throws ChannelStateException {

    throw new ChannelStateException("cannot remove persistent channel");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testInvalidPathExceptionCanBeThrown ()
    throws InvalidPathException {

    throw new InvalidPathException("path missing leading slash: %s", "foo/bar");
  }
}
