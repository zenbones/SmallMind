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
package org.smallmind.phalanx.worker;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.phalanx.CapturingAppender;
import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises {@link BindingAdvisory#warnIfUnbound(Class, String, PerApplicationScope)} directly across the four
 * combinations of the starting thread's bindings and the scope the owner was given. Only one combination warns:
 * no scope given, a scope bound on the thread, and no context attached.
 */
@Test(groups = "unit")
public class BindingAdvisoryTest {

  @Test
  public void testWarnsWhenScopeIsBoundWithoutAContextAndNoScopeWasGiven ()
    throws Exception {

    CapturingAppender appender = CapturingAppender.attach(BindingAdvisoryTest.class, Level.WARN);

    try {
      onIsolatedThread(() -> {
        new PerApplicationScope().run(() -> BindingAdvisory.warnIfUnbound(BindingAdvisoryTest.class, "Advisory owner", null));

        return null;
      });

      Assert.assertEquals(appender.size(), 1, "exactly one warning is expected, saw " + appender.getRecords());
      Assert.assertEquals(appender.getRecords().get(0).getLevel(), Level.WARN);
      Assert.assertTrue(appender.getRecords().get(0).getMessage().startsWith("Advisory owner"), appender.getRecords().get(0).getMessage());
      Assert.assertTrue(appender.getRecords().get(0).getMessage().contains("without being given that scope"), appender.getRecords().get(0).getMessage());
    } finally {
      CapturingAppender.release(BindingAdvisoryTest.class);
    }
  }

  @Test
  public void testDoesNotWarnWhenScopeIsBoundAndAContextIsAttached ()
    throws Exception {

    CapturingAppender appender = CapturingAppender.attach(BindingAdvisoryTest.class, Level.WARN);

    try {
      onIsolatedThread(() -> {
        new PerApplicationScope().run(() -> {
          new PerApplicationContext();
          BindingAdvisory.warnIfUnbound(BindingAdvisoryTest.class, "Advisory owner", null);
        });

        return null;
      });

      Assert.assertEquals(appender.size(), 0, "no warning is expected when a context is attached, saw " + appender.getRecords());
    } finally {
      CapturingAppender.release(BindingAdvisoryTest.class);
    }
  }

  @Test
  public void testDoesNotWarnWhenNeitherBindingIsPresent ()
    throws Exception {

    CapturingAppender appender = CapturingAppender.attach(BindingAdvisoryTest.class, Level.WARN);

    try {
      onIsolatedThread(() -> {
        Assert.assertFalse(PerApplicationScope.isBound(), "the thread must carry no scope");
        Assert.assertFalse(PerApplicationContext.isBound(), "the thread must carry no context");
        BindingAdvisory.warnIfUnbound(BindingAdvisoryTest.class, "Advisory owner", null);

        return null;
      });

      Assert.assertEquals(appender.size(), 0, "no warning is expected when neither binding is present, saw " + appender.getRecords());
    } finally {
      CapturingAppender.release(BindingAdvisoryTest.class);
    }
  }

  @Test
  public void testDoesNotWarnWhenAScopeWasGiven ()
    throws Exception {

    CapturingAppender appender = CapturingAppender.attach(BindingAdvisoryTest.class, Level.WARN);

    try {
      onIsolatedThread(() -> {

        PerApplicationScope perApplicationScope = new PerApplicationScope();

        perApplicationScope.run(() -> BindingAdvisory.warnIfUnbound(BindingAdvisoryTest.class, "Advisory owner", perApplicationScope));

        return null;
      });

      Assert.assertEquals(appender.size(), 0, "no warning is expected when the owner was given a scope, saw " + appender.getRecords());
    } finally {
      CapturingAppender.release(BindingAdvisoryTest.class);
    }
  }

  private static <T> T onIsolatedThread (Callable<T> body)
    throws Exception {

    AtomicReference<T> result = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {
        result.set(body.call());
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    if (failure.get() != null) {
      if (failure.get() instanceof Exception) {
        throw (Exception)failure.get();
      }
      throw (Error)failure.get();
    }

    return result.get();
  }
}
