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
package org.smallmind.scribe.pen.adapter;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.scribe.pen.Parameter;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the MDC-equivalent per-thread parameter store. The store is a process-wide singleton
 * backed by an {@link InheritableThreadLocal}, so each test clears it before and after to prevent
 * leakage across TestNG's reused worker threads.
 */
@Test(groups = "unit")
public class ParametersTest {

  @BeforeMethod
  public void clearBefore () {

    Parameters.getInstance().clear();
  }

  @AfterMethod
  public void clearAfter () {

    Parameters.getInstance().clear();
  }

  public void testPutAndGetRoundTrip () {

    Parameters.getInstance().put("requestId", "abc-123");

    Assert.assertEquals(Parameters.getInstance().get("requestId"), "abc-123");
  }

  public void testGetParametersSnapshot () {

    Parameters.getInstance().put("a", "1");
    Parameters.getInstance().put("b", "2");

    Parameter[] parameters = Parameters.getInstance().getParameters();
    Assert.assertEquals(parameters.length, 2);
  }

  public void testRemoveAndClear () {

    Parameters.getInstance().put("a", "1");
    Parameters.getInstance().put("b", "2");

    Parameters.getInstance().remove("a");
    Assert.assertNull(Parameters.getInstance().get("a"));
    Assert.assertEquals(Parameters.getInstance().get("b"), "2");

    Parameters.getInstance().clear();
    Assert.assertEquals(Parameters.getInstance().getParameters().length, 0);
  }

  public void testChildThreadInheritsParentParameters ()
    throws InterruptedException {

    Parameters.getInstance().put("tenant", "acme");

    AtomicReference<Serializable> seen = new AtomicReference<>();
    Thread child = new Thread(() -> seen.set(Parameters.getInstance().get("tenant")));

    child.start();
    child.join();

    Assert.assertEquals(seen.get(), "acme");
  }
}
