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
package org.smallmind.quorum.pool.complex;

import org.smallmind.quorum.pool.simple.SimplePoolConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ComplexPoolConfigTest {

  public void testDeconstructionIsNotRequiredWhenNoTimeoutIsSet () {

    Assert.assertFalse(new ComplexPoolConfig().requiresDeconstruction());
  }

  public void testAnyPositiveTimeoutRequiresDeconstruction () {

    Assert.assertTrue(new ComplexPoolConfig().setMaxLeaseTimeSeconds(1).requiresDeconstruction(), "a lease limit should require deconstruction");
    Assert.assertTrue(new ComplexPoolConfig().setMaxIdleTimeSeconds(1).requiresDeconstruction(), "an idle limit should require deconstruction");
    Assert.assertTrue(new ComplexPoolConfig().setMaxProcessingTimeSeconds(1).requiresDeconstruction(), "a processing limit should require deconstruction");
  }

  public void testNegativeValuesAreRejected () {

    ComplexPoolConfig config = new ComplexPoolConfig();

    Assert.assertThrows(IllegalArgumentException.class, () -> config.setInitialPoolSize(-1));
    Assert.assertThrows(IllegalArgumentException.class, () -> config.setMinPoolSize(-1));
    Assert.assertThrows(IllegalArgumentException.class, () -> config.setCreationTimeoutMillis(-1L));
    Assert.assertThrows(IllegalArgumentException.class, () -> config.setMaxLeaseTimeSeconds(-1));
    Assert.assertThrows(IllegalArgumentException.class, () -> config.setMaxIdleTimeSeconds(-1));
    Assert.assertThrows(IllegalArgumentException.class, () -> config.setMaxProcessingTimeSeconds(-1));
  }

  public void testCopyConstructorReproducesComplexProperties () {

    ComplexPoolConfig source = new ComplexPoolConfig()
                                 .setReportLeaseTimeNanos(true)
                                 .setTestOnCreate(true)
                                 .setTestOnAcquire(true)
                                 .setExistentiallyAware(true)
                                 .setCreationTimeoutMillis(1000L)
                                 .setInitialPoolSize(3)
                                 .setMinPoolSize(2)
                                 .setMaxLeaseTimeSeconds(60)
                                 .setMaxIdleTimeSeconds(30)
                                 .setMaxProcessingTimeSeconds(45);
    source.setMaxPoolSize(20).setAcquireWaitTimeMillis(750L);

    ComplexPoolConfig copy = new ComplexPoolConfig(source);

    Assert.assertEquals(copy.getMaxPoolSize(), 20);
    Assert.assertEquals(copy.getAcquireWaitTimeMillis(), 750L);
    Assert.assertTrue(copy.isReportLeaseTimeNanos());
    Assert.assertTrue(copy.isTestOnCreate());
    Assert.assertTrue(copy.isTestOnAcquire());
    Assert.assertTrue(copy.isExistentiallyAware());
    Assert.assertEquals(copy.getCreationTimeoutMillis(), 1000L);
    Assert.assertEquals(copy.getInitialPoolSize(), 3);
    Assert.assertEquals(copy.getMinPoolSize(), 2);
    Assert.assertEquals(copy.getMaxLeaseTimeSeconds(), 60);
    Assert.assertEquals(copy.getMaxIdleTimeSeconds(), 30);
    Assert.assertEquals(copy.getMaxProcessingTimeSeconds(), 45);
  }

  public void testCopyConstructorFromNonComplexSourceCopiesOnlyBaseProperties () {

    // The complex-specific copy block is guarded by an assignability check, so a plain base
    // configuration contributes only its base properties and the complex fields keep their defaults.
    SimplePoolConfig source = new SimplePoolConfig().setMaxPoolSize(33).setAcquireWaitTimeMillis(250L);

    ComplexPoolConfig copy = new ComplexPoolConfig(source);

    Assert.assertEquals(copy.getMaxPoolSize(), 33);
    Assert.assertEquals(copy.getAcquireWaitTimeMillis(), 250L);
    Assert.assertFalse(copy.requiresDeconstruction());
    Assert.assertFalse(copy.isTestOnCreate());
    Assert.assertEquals(copy.getInitialPoolSize(), 0);
    Assert.assertEquals(copy.getMaxProcessingTimeSeconds(), 0);
  }
}
