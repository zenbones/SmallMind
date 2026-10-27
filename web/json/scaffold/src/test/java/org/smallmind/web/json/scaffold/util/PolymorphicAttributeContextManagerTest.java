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
package org.smallmind.web.json.scaffold.util;

import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Covers {@link PolymorphicAttributeContextManager}, which stores the global polymorphic discriminator
 * attribute name in a {@link PerApplicationContext}. A fresh context is attached per method because
 * the manager resolves its value through the per-application data registry.
 */
@Test(groups = "unit")
public class PolymorphicAttributeContextManagerTest {

  @BeforeMethod
  public void beforeMethod () {

    new PerApplicationContext();
  }

  @AfterMethod
  public void afterMethod () {

    // The thread's context map outlives each test, so restore the default rather than leak a custom name into
    // whatever runs next on this thread.
    PolymorphicAttributeContextManager.setPolymorphicAttributeName(AttributedPolymorphicXmlAdapter.getDefaultPolymorphicAttributeName());
  }

  public void testDefaultAttributeNameIsReturnedWhenNothingIsStored ()
    throws InterruptedException {

    // The test thread's context map is shared across every test on the thread, so a fresh context can only be
    // observed on a thread that does not inherit it.
    AtomicReference<String> attributeName = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      new PerApplicationContext();
      attributeName.set(PolymorphicAttributeContextManager.getPolymorphicAttributeName());
    });

    thread.start();
    thread.join();

    Assert.assertEquals(attributeName.get(), AttributedPolymorphicXmlAdapter.getDefaultPolymorphicAttributeName());
  }

  public void testSetAndGetRoundTrip () {

    PolymorphicAttributeContextManager.setPolymorphicAttributeName("@kind");

    Assert.assertEquals(PolymorphicAttributeContextManager.getPolymorphicAttributeName(), "@kind");
  }
}
