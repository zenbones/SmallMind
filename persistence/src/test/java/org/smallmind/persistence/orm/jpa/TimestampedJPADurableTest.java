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
package org.smallmind.persistence.orm.jpa;

import java.time.LocalDateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link TimestampedJPADurable}. The creation and update timestamps are populated by the persistence
 * provider in production, so the unit-testable contract here is the timestamp accessors and the {@code mirrors}
 * override, which must exclude the {@code id}, {@code created}, and {@code lastUpdated} fields from the comparison
 * so that two records differing only in identity and provider-managed timestamps still mirror.
 */
@Test(groups = "unit")
public class TimestampedJPADurableTest {

  public void testMirrorsIgnoresIdAndTimestamps () {

    Gadget first = new Gadget();
    first.setId(1L);
    first.setCreated(LocalDateTime.of(2001, 1, 1, 0, 0));
    first.setLastUpdated(LocalDateTime.of(2001, 1, 2, 0, 0));
    first.setName("alpha");

    Gadget second = new Gadget();
    second.setId(2L);
    second.setCreated(LocalDateTime.of(2020, 6, 6, 6, 6));
    second.setLastUpdated(LocalDateTime.of(2021, 7, 7, 7, 7));
    second.setName("alpha");

    Assert.assertTrue(first.mirrors(second), "records differing only in id and timestamps should mirror");
  }

  public void testMirrorsDetectsADifferingBusinessField () {

    Gadget first = new Gadget();
    first.setId(1L);
    first.setName("alpha");

    Gadget second = new Gadget();
    second.setId(1L);
    second.setName("beta");

    Assert.assertFalse(first.mirrors(second), "a differing non-excluded field should break the mirror");
  }

  public void testTimestampAccessorsRoundTrip () {

    Gadget gadget = new Gadget();
    LocalDateTime created = LocalDateTime.of(2001, 2, 3, 4, 5);
    LocalDateTime lastUpdated = LocalDateTime.of(2002, 3, 4, 5, 6);

    gadget.setCreated(created);
    gadget.setLastUpdated(lastUpdated);

    Assert.assertEquals(gadget.getCreated(), created);
    Assert.assertEquals(gadget.getLastUpdated(), lastUpdated);
  }

  public static class Gadget extends TimestampedJPADurable<Long, Gadget> {

    private String name;

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }
  }
}
