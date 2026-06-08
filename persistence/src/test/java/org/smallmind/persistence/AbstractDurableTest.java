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
package org.smallmind.persistence;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.smallmind.nutsnbolts.lang.TypeMismatchException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class AbstractDurableTest {

  public void testEqualsMatchesByIdWhenBothPresent () {

    Assert.assertEquals(new Widget(1L, "alpha"), new Widget(1L, "beta"));
    Assert.assertNotEquals(new Widget(1L, "alpha"), new Widget(2L, "alpha"));
  }

  public void testEqualsFallsBackToIdentityWhenEitherIdIsNull () {

    Widget transientWidget = new Widget(null, "alpha");

    Assert.assertEquals(transientWidget, transientWidget);
    Assert.assertNotEquals(transientWidget, new Widget(null, "alpha"));
    Assert.assertNotEquals(transientWidget, new Widget(1L, "alpha"));
  }

  public void testEqualsReturnsFalseForNonDurable () {

    Assert.assertNotEquals(new Widget(1L, "alpha"), "1");
  }

  public void testEqualObjectsShareHashCode () {

    Assert.assertEquals(new Widget(7L, "alpha").hashCode(), new Widget(7L, "beta").hashCode());
  }

  public void testHashCodeIsStableForTransientInstance () {

    Widget transientWidget = new Widget(null, "alpha");

    Assert.assertEquals(transientWidget.hashCode(), transientWidget.hashCode());
  }

  public void testCompareToOrdersByIdDescending () {

    Widget low = new Widget(1L, "alpha");
    Widget high = new Widget(2L, "alpha");

    Assert.assertTrue(low.compareTo(high) > 0);
    Assert.assertTrue(high.compareTo(low) < 0);
    Assert.assertEquals(low.compareTo(new Widget(1L, "beta")), 0);
  }

  public void testSortPlacesLargestIdFirst () {

    List<Widget> widgets = new ArrayList<>();

    widgets.add(new Widget(2L, "b"));
    widgets.add(new Widget(1L, "a"));
    widgets.add(new Widget(3L, "c"));

    Collections.sort(widgets);

    Assert.assertEquals(widgets.get(0).getId(), Long.valueOf(3L));
    Assert.assertEquals(widgets.get(1).getId(), Long.valueOf(2L));
    Assert.assertEquals(widgets.get(2).getId(), Long.valueOf(1L));
  }

  public void testCompareToSortsNullIdFirst () {

    Widget transientWidget = new Widget(null, "alpha");
    Widget persistentWidget = new Widget(1L, "alpha");

    Assert.assertEquals(transientWidget.compareTo(persistentWidget), -1);
    Assert.assertEquals(persistentWidget.compareTo(transientWidget), 1);
    Assert.assertEquals(transientWidget.compareTo(new Widget(null, "beta")), 0);
  }

  @Test(groups = "unit", expectedExceptions = TypeMismatchException.class)
  public void testCompareToRejectsIncompatibleType () {

    new Widget(1L, "alpha").compareTo(new Gadget(1L));
  }

  public void testMirrorsComparesEveryFieldExceptId () {

    Assert.assertTrue(new Widget(1L, "alpha").mirrors(new Widget(2L, "alpha")));
    Assert.assertFalse(new Widget(1L, "alpha").mirrors(new Widget(2L, "beta")));
  }

  public void testMirrorsHandlesNullFieldValues () {

    Assert.assertTrue(new Widget(1L, null).mirrors(new Widget(2L, null)));
    Assert.assertFalse(new Widget(1L, null).mirrors(new Widget(2L, "beta")));
  }

  public void testMirrorsReturnsFalseForUnassignableType () {

    Assert.assertFalse(new Widget(1L, "alpha").mirrors(new Gadget(1L)));
  }

  @Test(groups = "unit", expectedExceptions = PersistenceException.class)
  public void testMirrorsRejectsExclusionFromForeignClass ()
    throws NoSuchFieldException {

    Field foreignField = Foreign.class.getDeclaredField("value");

    new Widget(1L, "alpha").mirrors(new Widget(1L, "alpha"), foreignField);
  }

  public void testToStringRendersFieldsAndGuardsCycles () {

    Widget widget = new Widget(1L, "alpha");

    widget.setPeer(widget);

    String rendered = widget.toString();

    Assert.assertTrue(rendered.startsWith("Widget["));
    Assert.assertTrue(rendered.contains("name=alpha"));
    Assert.assertTrue(rendered.contains("peer=Widget[id=1,...]"));
  }

  public static class Widget extends AbstractDurable<Long, Widget> {

    private Long id;
    private String name;
    private Widget peer;

    public Widget () {

    }

    public Widget (Long id, String name) {

      this.id = id;
      this.name = name;
    }

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }

    public Widget getPeer () {

      return peer;
    }

    public void setPeer (Widget peer) {

      this.peer = peer;
    }
  }

  public static class Gadget extends AbstractDurable<Long, Gadget> {

    private Long id;

    public Gadget (Long id) {

      this.id = id;
    }

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }

  public static class Foreign {

    private int value;
  }
}
