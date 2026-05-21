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
package org.smallmind.bayeux.oumuamua.server.spi;

import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class AbstractAttributedTest {

  private AbstractAttributed attributed;

  @BeforeMethod
  public void beforeMethod () {

    attributed = new AbstractAttributed();
  }

  public void testGetAttributeAbsentReturnsNull () {

    Assert.assertNull(attributed.getAttribute("anything"));
  }

  public void testSetOverwritesPreviousValue () {

    attributed.setAttribute("k", "first");
    attributed.setAttribute("k", "second");

    Assert.assertEquals(attributed.getAttribute("k"), "second");
  }

  public void testSetAllowsArbitraryObjectTypes () {

    Object marker = new Object();

    attributed.setAttribute("string", "text");
    attributed.setAttribute("integer", 42);
    attributed.setAttribute("object", marker);

    Assert.assertEquals(attributed.getAttribute("string"), "text");
    Assert.assertEquals(attributed.getAttribute("integer"), 42);
    Assert.assertSame(attributed.getAttribute("object"), marker);
  }

  public void testRemoveReturnsPreviousValue () {

    attributed.setAttribute("k", "v");

    Assert.assertEquals(attributed.removeAttribute("k"), "v");
    Assert.assertNull(attributed.getAttribute("k"));
  }

  public void testRemoveAbsentReturnsNull () {

    Assert.assertNull(attributed.removeAttribute("absent"));
  }

  public void testGetAttributeNamesInitiallyEmpty () {

    Assert.assertTrue(attributed.getAttributeNames().isEmpty());
  }

  public void testGetAttributeNamesReflectsAdded () {

    attributed.setAttribute("a", 1);
    attributed.setAttribute("b", 2);
    attributed.setAttribute("c", 3);

    Set<String> names = attributed.getAttributeNames();

    Assert.assertEquals(names.size(), 3);
    Assert.assertTrue(names.contains("a"));
    Assert.assertTrue(names.contains("b"));
    Assert.assertTrue(names.contains("c"));
  }

  public void testGetAttributeNamesExcludesRemoved () {

    attributed.setAttribute("a", 1);
    attributed.setAttribute("b", 2);
    attributed.removeAttribute("a");

    Set<String> names = attributed.getAttributeNames();

    Assert.assertEquals(names.size(), 1);
    Assert.assertFalse(names.contains("a"));
    Assert.assertTrue(names.contains("b"));
  }

  public void testGetAttributeNamesIsSnapshot () {

    attributed.setAttribute("a", 1);
    Set<String> names = attributed.getAttributeNames();

    attributed.setAttribute("b", 2);

    Assert.assertEquals(names.size(), 1);
    Assert.assertFalse(names.contains("b"));
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testGetAttributeNamesIsUnmodifiable () {

    attributed.setAttribute("a", 1);
    attributed.getAttributeNames().add("b");
  }

  public void testIndependentAttributesCoexist () {

    attributed.setAttribute("a", 1);
    attributed.setAttribute("b", 2);
    attributed.removeAttribute("a");

    Assert.assertNull(attributed.getAttribute("a"));
    Assert.assertEquals(attributed.getAttribute("b"), 2);
  }
}
