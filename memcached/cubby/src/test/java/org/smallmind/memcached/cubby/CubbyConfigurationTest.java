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
package org.smallmind.memcached.cubby;

import org.smallmind.memcached.cubby.codec.LargeValueCompressingCodec;
import org.smallmind.memcached.cubby.codec.ObjectStreamCubbyCodec;
import org.smallmind.memcached.cubby.locator.DefaultKeyLocator;
import org.smallmind.memcached.cubby.locator.MaglevKeyLocator;
import org.smallmind.memcached.cubby.translator.DefaultKeyTranslator;
import org.smallmind.memcached.cubby.translator.LargeKeyHashingTranslator;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CubbyConfigurationTest {

  public void testDefaultPresetUsesPlainObjectStreamCodecAndModuloLocator () {

    CubbyConfiguration configuration = CubbyConfiguration.DEFAULT;

    Assert.assertTrue(configuration.getCodec() instanceof ObjectStreamCubbyCodec, configuration.getCodec().getClass().getName());
    Assert.assertTrue(configuration.getKeyLocator() instanceof DefaultKeyLocator, configuration.getKeyLocator().getClass().getName());
    Assert.assertTrue(configuration.getKeyTranslator() instanceof DefaultKeyTranslator, configuration.getKeyTranslator().getClass().getName());
    Assert.assertNull(configuration.getAuthentication());
  }

  public void testOptimalPresetUsesCompressingCodecMaglevLocatorAndHashingTranslator () {

    CubbyConfiguration configuration = CubbyConfiguration.OPTIMAL;

    Assert.assertTrue(configuration.getCodec() instanceof LargeValueCompressingCodec, configuration.getCodec().getClass().getName());
    Assert.assertTrue(configuration.getKeyLocator() instanceof MaglevKeyLocator, configuration.getKeyLocator().getClass().getName());
    Assert.assertTrue(configuration.getKeyTranslator() instanceof LargeKeyHashingTranslator, configuration.getKeyTranslator().getClass().getName());
  }

  public void testDefaultsForTimeoutAndPoolKnobsMatchDocumentedValues () {

    CubbyConfiguration configuration = new CubbyConfiguration();

    Assert.assertEquals(configuration.getDefaultRequestTimeoutMilliseconds(), 0L);
    Assert.assertEquals(configuration.getConnectionTimeoutMilliseconds(), 3000L);
    Assert.assertEquals(configuration.getReadTimeoutMilliseconds(), 30000L);
    Assert.assertEquals(configuration.getKeepAliveSeconds(), 30L);
    Assert.assertEquals(configuration.getResuscitationSeconds(), 10L);
    Assert.assertEquals(configuration.getConnectionsPerHost(), 1);
  }
}
