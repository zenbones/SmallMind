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
package org.smallmind.nutsnbolts.reflection.bean;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class BeanUtilityTest {

  public void testAsGetterNameProducesGetXxx () {

    Assert.assertEquals(BeanUtility.asGetterName("name"), "getName");
  }

  public void testAsSetterNameProducesSetXxx () {

    Assert.assertEquals(BeanUtility.asSetterName("name"), "setName");
  }

  public void testAsIsNameProducesIsXxx () {

    Assert.assertEquals(BeanUtility.asIsName("active"), "isActive");
  }

  public void testExecuteGetInvokesGetter ()
    throws Exception {

    Person person = new Person();

    person.setName("Alice");

    Assert.assertEquals(BeanUtility.executeGet(person, "name", false), "Alice");
  }

  public void testExecuteGetFallsBackToIsForBoolean ()
    throws Exception {

    Person person = new Person();

    person.setActive(true);

    Assert.assertEquals(BeanUtility.executeGet(person, "active", false), Boolean.TRUE);
  }

  public void testExecuteGetTraversesDottedPath ()
    throws Exception {

    Person person = new Person();
    Address address = new Address();

    address.setCity("Paris");
    person.setAddress(address);

    Assert.assertEquals(BeanUtility.executeGet(person, "address.city", false), "Paris");
  }

  public void testExecuteGetNullableReturnsNullForMissingIntermediate ()
    throws Exception {

    Assert.assertNull(BeanUtility.executeGet(new Person(), "address.city", true));
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testExecuteGetNonNullableThrowsForMissingIntermediate ()
    throws Exception {

    BeanUtility.executeGet(new Person(), "address.city", false);
  }

  public void testExecuteSetInvokesSetter ()
    throws Exception {

    Person person = new Person();

    BeanUtility.executeSet(person, "name", "Bob");

    Assert.assertEquals(person.getName(), "Bob");
  }

  public void testExecuteSetTraversesDottedPath ()
    throws Exception {

    Person person = new Person();
    Address address = new Address();

    person.setAddress(address);
    BeanUtility.executeSet(person, "address.city", "Lyon");

    Assert.assertEquals(address.getCity(), "Lyon");
  }

  public void testExecuteInvokesArbitraryMethodOnPath ()
    throws Exception {

    Person person = new Person();

    person.setName("Carol");

    Assert.assertEquals(BeanUtility.execute(person, "greet", "Hi"), "Hi Carol");
  }

  @Test(expectedExceptions = BeanAccessException.class)
  public void testMissingGetterThrowsBeanAccessException ()
    throws Exception {

    BeanUtility.executeGet(new Person(), "nonexistent", false);
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

  public static class Person {

    private String name;
    private Address address;
    private boolean active;

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }

    public Address getAddress () {

      return address;
    }

    public void setAddress (Address address) {

      this.address = address;
    }

    public boolean isActive () {

      return active;
    }

    public void setActive (boolean active) {

      this.active = active;
    }

    public String greet (String prefix) {

      return prefix + " " + name;
    }
  }
}
