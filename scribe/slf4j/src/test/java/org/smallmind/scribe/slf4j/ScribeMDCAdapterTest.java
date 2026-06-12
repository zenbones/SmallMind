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
package org.smallmind.scribe.slf4j;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.smallmind.scribe.pen.adapter.Parameters;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ScribeMDCAdapterTest {

  private ScribeMDCAdapter adapter;

  @BeforeMethod
  public void before () {

    Parameters.getInstance().clear();
    adapter = new ScribeMDCAdapter();
  }

  @AfterMethod
  public void after () {

    Parameters.getInstance().clear();
  }

  public void testPutGetRoundTrip () {

    adapter.put("alpha", "one");
    Assert.assertEquals(adapter.get("alpha"), "one");
  }

  public void testGetMissingKeyReturnsNull () {

    Assert.assertNull(adapter.get("nope"));
  }

  public void testPutOverwritesExistingValue () {

    adapter.put("alpha", "one");
    adapter.put("alpha", "two");
    Assert.assertEquals(adapter.get("alpha"), "two");
  }

  public void testRemove () {

    adapter.put("alpha", "one");
    adapter.remove("alpha");
    Assert.assertNull(adapter.get("alpha"));
  }

  public void testClear () {

    adapter.put("alpha", "one");
    adapter.put("beta", "two");
    adapter.clear();
    Assert.assertNull(adapter.get("alpha"));
    Assert.assertNull(adapter.get("beta"));
  }

  public void testGetCopyOfContextMap () {

    adapter.put("alpha", "one");
    adapter.put("beta", "two");

    Map<String, String> map = adapter.getCopyOfContextMap();

    Assert.assertEquals(map.size(), 2);
    Assert.assertEquals(map.get("alpha"), "one");
    Assert.assertEquals(map.get("beta"), "two");
  }

  public void testGetCopyOfContextMapIsSnapshot () {

    adapter.put("alpha", "one");

    Map<String, String> map = adapter.getCopyOfContextMap();

    adapter.put("beta", "two");

    Assert.assertEquals(map.size(), 1);
    Assert.assertFalse(map.containsKey("beta"));
  }

  public void testSetContextMapReplacesEverything () {

    adapter.put("stale", "gone");

    Map<String, String> replacement = new HashMap<>();
    replacement.put("fresh", "here");
    adapter.setContextMap(replacement);

    Assert.assertNull(adapter.get("stale"));
    Assert.assertEquals(adapter.get("fresh"), "here");
  }

  public void testPushAndPopByKey () {

    adapter.pushByKey("stack", "first");
    adapter.pushByKey("stack", "second");

    Assert.assertEquals(adapter.popByKey("stack"), "first");
    Assert.assertEquals(adapter.popByKey("stack"), "second");
    Assert.assertNull(adapter.popByKey("stack"));
  }

  public void testPopByKeyMissingReturnsNull () {

    Assert.assertNull(adapter.popByKey("absent"));
  }

  public void testPopByKeyScalarRemovesEntry () {

    adapter.put("scalar", "value");

    Assert.assertNull(adapter.popByKey("scalar"));
    Assert.assertNull(adapter.get("scalar"));
  }

  public void testGetCopyOfDequeByKey () {

    adapter.pushByKey("stack", "first");
    adapter.pushByKey("stack", "second");

    Deque<String> copy = adapter.getCopyOfDequeByKey("stack");

    Assert.assertNotNull(copy);
    Assert.assertEquals(copy.size(), 2);
    Assert.assertTrue(copy.contains("first"));
    Assert.assertTrue(copy.contains("second"));
  }

  public void testGetCopyOfDequeByKeyMissingReturnsNull () {

    Assert.assertNull(adapter.getCopyOfDequeByKey("absent"));
  }

  public void testGetCopyOfDequeByKeyScalarReturnsNull () {

    adapter.put("scalar", "value");
    Assert.assertNull(adapter.getCopyOfDequeByKey("scalar"));
  }

  public void testClearDequeByKey () {

    adapter.pushByKey("stack", "first");
    adapter.clearDequeByKey("stack");

    Deque<String> copy = adapter.getCopyOfDequeByKey("stack");

    Assert.assertNotNull(copy);
    Assert.assertTrue(copy.isEmpty());
    Assert.assertNull(adapter.popByKey("stack"));
  }
}
