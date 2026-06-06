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
package org.smallmind.nutsnbolts.reflection;

import java.lang.reflect.Field;
import org.smallmind.nutsnbolts.lang.TypeMismatchException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class OverlayTest {

  public void testOverlayCopiesNonNullFieldsAndLeavesNullFieldsAlone () {

    Sample target = new Sample();
    target.setName("original");
    target.setCount(7);

    Sample source = new Sample();
    source.setName("replacement");

    target.overlay(source);

    Assert.assertEquals(target.getName(), "replacement");
    Assert.assertEquals(target.getCount().intValue(), 7);
  }

  public void testOverlayNullifierConvertsEmptyStringToNull () {

    Sample target = new Sample();
    target.setTag("kept");

    Sample source = new Sample();
    source.setTag("");

    target.overlay(source);

    Assert.assertNull(target.getTag(), "Empty-string nullifier should have wiped tag");
  }

  public void testOverlaidCallbackInvokedAfterApply () {

    Sample target = new Sample();
    target.overlay(new Sample());

    Assert.assertTrue(target.isOverlaidCalled());
  }

  public void testOverlayWithNullSourceStillTriggersOverlaidCallback () {

    Sample target = new Sample();

    target.overlay((Sample)null);

    Assert.assertTrue(target.isOverlaidCalled());
  }

  public void testOverlayArrayAppliesEachSourceInOrder () {

    Sample target = new Sample();
    Sample first = new Sample();
    first.setName("first-name");
    Sample second = new Sample();
    second.setCount(42);

    target.overlay(new Sample[] {first, second});

    Assert.assertEquals(target.getName(), "first-name");
    Assert.assertEquals(target.getCount().intValue(), 42);
  }

  public void testOverlayArraySkipsNullEntries () {

    Sample target = new Sample();
    Sample source = new Sample();
    source.setName("only");

    target.overlay(new Sample[] {null, source, null});

    Assert.assertEquals(target.getName(), "only");
  }

  public void testOverlayWithExclusionPreservesExcludedField ()
    throws NoSuchFieldException {

    Sample target = new Sample();
    target.setName("keep-me");

    Sample source = new Sample();
    source.setName("overwrite");
    source.setCount(9);

    Field nameField = Sample.class.getDeclaredField("name");

    target.overlay(source, new Field[] {nameField});

    Assert.assertEquals(target.getName(), "keep-me");
    Assert.assertEquals(target.getCount().intValue(), 9);
  }

  @Test(expectedExceptions = TypeMismatchException.class)
  public void testExclusionFieldFromUnrelatedClassThrows ()
    throws NoSuchFieldException {

    Sample target = new Sample();
    Sample source = new Sample();
    source.setName("replacement");

    Field strayField = String.class.getDeclaredField("hash");

    target.overlay(source, new Field[] {strayField});
  }

  public void testOverlayArrayWithEmptyArrayReturnsTargetUnchanged () {

    Sample target = new Sample();
    target.setName("untouched");

    target.overlay(new Sample[0]);

    Assert.assertEquals(target.getName(), "untouched");
  }

  public static class Sample implements Overlay<Sample> {

    private String name;
    @EmptyStringNullifier
    private String tag;
    private Integer count;
    private boolean overlaidCalled;

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }

    public String getTag () {

      return tag;
    }

    public void setTag (String tag) {

      this.tag = tag;
    }

    public Integer getCount () {

      return count;
    }

    public void setCount (Integer count) {

      this.count = count;
    }

    public boolean isOverlaidCalled () {

      return overlaidCalled;
    }

    @Override
    public void overlaid () {

      overlaidCalled = true;
    }
  }

  public static class Unrelated {

  }
}
