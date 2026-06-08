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
package org.smallmind.persistence.orm.throng;

import java.time.LocalDateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link TimestampedThrongDurable}, whose {@code @PrePersist} hook stamps the creation timestamp
 * once and bumps the update timestamp on every persist. The hook is invoked directly (no MongoDB involved) over a
 * minimal concrete subclass.
 */
@Test(groups = "unit")
public class TimestampedThrongDurableTest {

  public void testPrePersistStampsBothTimestamps () {

    Gadget gadget = new Gadget();

    gadget.prePersist();

    Assert.assertNotNull(gadget.getCreated(), "prePersist should set the creation timestamp");
    Assert.assertNotNull(gadget.getLastUpdated(), "prePersist should set the update timestamp");
  }

  public void testPrePersistKeepsCreatedButBumpsLastUpdated ()
    throws InterruptedException {

    Gadget gadget = new Gadget();

    gadget.prePersist();

    LocalDateTime firstCreated = gadget.getCreated();
    LocalDateTime firstUpdated = gadget.getLastUpdated();

    // LocalDateTime.now() must advance between the two stamps for the bump to be observable.
    Thread.sleep(5);

    gadget.prePersist();

    Assert.assertEquals(gadget.getCreated(), firstCreated, "a second persist must not move the creation timestamp");
    Assert.assertTrue(gadget.getLastUpdated().isAfter(firstUpdated), "a second persist must bump the update timestamp");
  }

  public void testPrePersistPreservesAPreSetCreatedTimestamp () {

    Gadget gadget = new Gadget();
    LocalDateTime preset = LocalDateTime.of(2000, 1, 1, 0, 0);

    gadget.setCreated(preset);
    gadget.prePersist();

    Assert.assertEquals(gadget.getCreated(), preset, "an explicitly set creation timestamp must be preserved");
    Assert.assertNotNull(gadget.getLastUpdated());
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

  public static class Gadget extends TimestampedThrongDurable<Long, Gadget> {

  }
}
