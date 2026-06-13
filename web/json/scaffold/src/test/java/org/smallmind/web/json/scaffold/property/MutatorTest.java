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
package org.smallmind.web.json.scaffold.property;

import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers {@link ArrayMutator} and {@link ListMutator}, which reflectively convert between view and
 * entity objects via a view-instance {@code factory()} method and a static view {@code instance(entity)}
 * method, including the null and missing-method ({@link PropertyException}) paths.
 */
@Test(groups = "unit")
public class MutatorTest {

  public void testArrayToEntityType ()
    throws PropertyException {

    Gadget[] gadgets = ArrayMutator.toEntityType(Gadget.class, new GadgetView[] {new GadgetView("a"), new GadgetView("b")});

    Assert.assertEquals(gadgets.length, 2);
    Assert.assertEquals(gadgets[0].getCode(), "a");
    Assert.assertEquals(gadgets[1].getCode(), "b");
  }

  public void testArrayToViewType ()
    throws PropertyException {

    GadgetView[] views = ArrayMutator.toViewType(Gadget.class, GadgetView.class, new Gadget[] {new Gadget("x"), new Gadget("y")});

    Assert.assertEquals(views.length, 2);
    Assert.assertEquals(views[0].getCode(), "x");
    Assert.assertEquals(views[1].getCode(), "y");
  }

  public void testArrayNullPassThrough ()
    throws PropertyException {

    Assert.assertNull(ArrayMutator.toEntityType(Gadget.class, (GadgetView[])null));
    Assert.assertNull(ArrayMutator.toViewType(Gadget.class, GadgetView.class, (Gadget[])null));
  }

  @Test(expectedExceptions = PropertyException.class)
  public void testArrayMissingFactoryMethodFails ()
    throws PropertyException {

    // String has no factory() method, so the reflective lookup fails.
    ArrayMutator.toEntityType(Gadget.class, new String[] {"no-factory"});
  }

  public void testListToEntityType ()
    throws PropertyException {

    List<Gadget> gadgets = ListMutator.toEntityType(List.of(new GadgetView("a"), new GadgetView("b")));

    Assert.assertEquals(gadgets.size(), 2);
    Assert.assertEquals(gadgets.get(0).getCode(), "a");
  }

  public void testListToViewType ()
    throws PropertyException {

    List<GadgetView> views = ListMutator.toViewType(Gadget.class, GadgetView.class, List.of(new Gadget("x"), new Gadget("y")));

    Assert.assertEquals(views.size(), 2);
    Assert.assertEquals(views.get(1).getCode(), "y");
  }

  public void testListNullPassThrough ()
    throws PropertyException {

    Assert.assertNull(ListMutator.toEntityType(null));
    Assert.assertNull(ListMutator.toViewType(Gadget.class, GadgetView.class, null));
  }

  @Test(expectedExceptions = PropertyException.class)
  public void testListMissingFactoryMethodFails ()
    throws PropertyException {

    // String has no factory() method, so the reflective lookup fails inside the conversion.
    ListMutator.toEntityType(List.of("no-factory"));
  }

  @Test(expectedExceptions = PropertyException.class)
  public void testListMissingInstanceMethodFails ()
    throws PropertyException {

    // GadgetView exposes instance(Gadget), not instance(String), so the lookup fails.
    ListMutator.toViewType(String.class, GadgetView.class, List.of("no-instance"));
  }

  public static class Gadget {

    private String code;

    public Gadget () {

    }

    public Gadget (String code) {

      this.code = code;
    }

    public String getCode () {

      return code;
    }
  }

  public static class GadgetView {

    private String code;

    public GadgetView () {

    }

    public GadgetView (String code) {

      this.code = code;
    }

    public static GadgetView instance (Gadget gadget) {

      return new GadgetView(gadget.getCode());
    }

    public Gadget factory () {

      return new Gadget(code);
    }

    public String getCode () {

      return code;
    }
  }
}
