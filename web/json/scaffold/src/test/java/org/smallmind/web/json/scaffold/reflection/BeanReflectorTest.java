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
package org.smallmind.web.json.scaffold.reflection;

import org.smallmind.nutsnbolts.reflection.bean.BeanAccessException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises {@link BeanReflector} getter, setter, and method-invocation traversal over a small nested
 * bean graph, including array subscripts, {@code isX} boolean accessors, and failure conditions.
 */
@Test(groups = "unit")
public class BeanReflectorTest {

  private Order buildOrder () {

    Address address = new Address();
    address.setCity("Portland");

    Item first = new Item();
    first.setQuantity(1);
    Item second = new Item();
    second.setQuantity(2);

    Order order = new Order();
    order.setBillingAddress(address);
    order.setItems(new Item[] {first, second});
    order.setActive(true);

    return order;
  }

  public void testGetNestedProperty ()
    throws BeanAccessException {

    Assert.assertEquals(BeanReflector.get(buildOrder(), "billingAddress.city"), "Portland");
  }

  public void testGetThroughArraySubscript ()
    throws BeanAccessException {

    Assert.assertEquals(BeanReflector.get(buildOrder(), "items[1].quantity"), 2);
  }

  public void testGetBooleanIsAccessor ()
    throws BeanAccessException {

    Assert.assertEquals(BeanReflector.get(buildOrder(), "active"), Boolean.TRUE);
  }

  public void testSetNestedProperty ()
    throws BeanAccessException {

    Order order = buildOrder();

    BeanReflector.set(order, "billingAddress.city", "Seattle");

    Assert.assertEquals(order.getBillingAddress().getCity(), "Seattle");
  }

  public void testSetThroughArraySubscript ()
    throws BeanAccessException {

    Order order = buildOrder();

    BeanReflector.set(order, "items[0].quantity", 9);

    Assert.assertEquals(order.getItems()[0].getQuantity(), 9);
  }

  public void testApplyInvokesMethod ()
    throws BeanAccessException {

    Assert.assertEquals(BeanReflector.apply(buildOrder(), "greet", "world"), "Hello world");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testGetOnNullIntermediateFails ()
    throws BeanAccessException {

    Order order = buildOrder();
    order.setBillingAddress(null);

    BeanReflector.get(order, "billingAddress.city");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testGetMissingPropertyFails ()
    throws BeanAccessException {

    BeanReflector.get(buildOrder(), "noSuchProperty");
  }

  public void testGetPublicFieldFallback ()
    throws BeanAccessException {

    Order order = buildOrder();
    order.note = "shipped";

    Assert.assertEquals(BeanReflector.get(order, "note"), "shipped");
  }

  public void testSetPublicFieldFallback ()
    throws BeanAccessException {

    Order order = buildOrder();

    BeanReflector.set(order, "note", "urgent");

    Assert.assertEquals(order.note, "urgent");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testSetOnNullIntermediateFails ()
    throws BeanAccessException {

    Order order = buildOrder();
    order.setBillingAddress(null);

    BeanReflector.set(order, "billingAddress.city", "Denver");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testApplyUnknownMethodFails ()
    throws BeanAccessException {

    BeanReflector.apply(buildOrder(), "noSuchMethod", "arg");
  }

  public static class Order {

    public String note;
    private Address billingAddress;
    private Item[] items;
    private boolean active;

    public Address getBillingAddress () {

      return billingAddress;
    }

    public void setBillingAddress (Address billingAddress) {

      this.billingAddress = billingAddress;
    }

    public Item[] getItems () {

      return items;
    }

    public void setItems (Item[] items) {

      this.items = items;
    }

    public boolean isActive () {

      return active;
    }

    public void setActive (boolean active) {

      this.active = active;
    }

    public String greet (String who) {

      return "Hello " + who;
    }
  }

  public static class Address {

    private String city;

    public String getCity () {

      return city;
    }

    public void setCity (String city) {

      this.city = city;
    }
  }

  public static class Item {

    private int quantity;

    public int getQuantity () {

      return quantity;
    }

    public void setQuantity (int quantity) {

      this.quantity = quantity;
    }
  }
}
