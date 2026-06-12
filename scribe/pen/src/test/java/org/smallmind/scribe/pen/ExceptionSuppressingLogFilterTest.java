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

import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * The suppression set behind {@link ExceptionSuppressingLogFilter} is process-global, additive, and
 * has no clear/remove API. These tests therefore register a private marker exception that no other
 * test references, so the shared state they add cannot perturb (or be perturbed by) anything else.
 */
@Test(groups = "unit")
public class ExceptionSuppressingLogFilterTest {

  private static class SuppressedMarkerException extends RuntimeException {

  }

  private static class SuppressedMarkerSubclassException extends SuppressedMarkerException {

  }

  private static class UnrelatedException extends RuntimeException {

  }

  @BeforeClass
  public void registerSuppressedType () {

    ExceptionSuppressingLogFilter.addSuppressedThrowableClasses(Collections.singletonList(SuppressedMarkerException.class));
  }

  public void testRecordsWithoutThrowableAlwaysPass () {

    Assert.assertTrue(new ExceptionSuppressingLogFilter().willLog(new RecordFixture().setThrown(null)));
  }

  public void testUnsuppressedThrowablePasses () {

    Assert.assertTrue(new ExceptionSuppressingLogFilter().willLog(new RecordFixture().setThrown(new UnrelatedException())));
  }

  public void testSuppressedThrowableIsBlocked () {

    Assert.assertFalse(new ExceptionSuppressingLogFilter().willLog(new RecordFixture().setThrown(new SuppressedMarkerException())));
  }

  public void testSubclassOfSuppressedTypeIsNotBlocked () {

    // Suppression keys on the exact class, not instanceof, so a subclass of a suppressed type passes.
    Assert.assertTrue(new ExceptionSuppressingLogFilter().willLog(new RecordFixture().setThrown(new SuppressedMarkerSubclassException())));
  }
}
