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
package org.smallmind.persistence.cache;

import org.smallmind.persistence.AbstractDurable;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class DurableKeyTest {

  public void testKeyIsClassNameJoinedToIdByEquals () {

    DurableKey<Long, Sample> key = new DurableKey<>(Sample.class, 42L);

    Assert.assertEquals(key.getKey(), "Sample=42");
    Assert.assertEquals(key.toString(), "Sample=42");
  }

  public void testGetIdAsStringReturnsPortionAfterEquals () {

    Assert.assertEquals(new DurableKey<>(Sample.class, 42L).getIdAsString(), "42");
  }

  public void testGetDurableClassReturnsConstructorArgument () {

    Assert.assertEquals(new DurableKey<>(Sample.class, 42L).getDurableClass(), Sample.class);
  }

  public void testEqualityAndHashCodeFollowKeyString () {

    DurableKey<Long, Sample> key = new DurableKey<>(Sample.class, 42L);

    Assert.assertEquals(key, new DurableKey<>(Sample.class, 42L));
    Assert.assertEquals(key.hashCode(), new DurableKey<>(Sample.class, 42L).hashCode());
    Assert.assertNotEquals(key, new DurableKey<>(Sample.class, 43L));
  }

  public void testKeysDifferWhenClassNamesDiffer () {

    Assert.assertNotEquals(new DurableKey<>(Sample.class, 42L), new DurableKey<>(Other.class, 42L));
  }

  public void testEqualsReturnsFalseForNonDurableKey () {

    Assert.assertNotEquals(new DurableKey<>(Sample.class, 42L), "Sample=42");
  }

  public static class Sample extends AbstractDurable<Long, Sample> {

    private Long id;

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }

  public static class Other extends AbstractDurable<Long, Other> {

    private Long id;

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }
}
