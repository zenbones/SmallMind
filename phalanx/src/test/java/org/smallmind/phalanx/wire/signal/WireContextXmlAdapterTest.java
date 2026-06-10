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
package org.smallmind.phalanx.wire.signal;

import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.phalanx.wire.WireContextManager;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;

/**
 * Verifies the JAXB {@link WireContextXmlAdapter} conversion contract: null and empty inputs collapse
 * to null or an empty array, a registered context type round-trips back to its concrete class, and an
 * unregistered type tag is preserved as a {@link ProtoWireContext} rather than being dropped. Context
 * type tags are unique per test to avoid cross-test bleed in the shared per-application registry.
 */
@Test(groups = "unit")
public class WireContextXmlAdapterTest {

  @BeforeClass
  public void beforeClass () {

    //  WireContextManager's handle-to-class registry lives in the per-application context; establish
    //  one on this thread before registering or resolving context types.
    new PerApplicationContext();
  }

  @Test
  public void testMarshalNullReturnsNull () {

    Assert.assertNull(new WireContextXmlAdapter().marshal(null));
  }

  @Test
  public void testMarshalEmptyReturnsNull () {

    Assert.assertNull(new WireContextXmlAdapter().marshal(new WireContext[0]));
  }

  @Test
  public void testUnmarshalNullReturnsEmptyArray () {

    Assert.assertEquals(new WireContextXmlAdapter().unmarshal(null).length, 0);
  }

  @Test
  public void testRegisteredTypeRoundTrips () {

    WireContextXmlAdapter adapter = new WireContextXmlAdapter();
    AdapterContext context = new AdapterContext();

    context.setValue("payload");
    WireContextManager.register("AdapterContext", AdapterContext.class);

    JsonNode node = adapter.marshal(new WireContext[] {context});
    WireContext[] unmarshalled = adapter.unmarshal(node);

    Assert.assertEquals(unmarshalled.length, 1);
    Assert.assertTrue(unmarshalled[0] instanceof AdapterContext);
    Assert.assertEquals(((AdapterContext)unmarshalled[0]).getValue(), "payload");
  }

  @Test
  public void testUnregisteredTypeBecomesProtoContext () {

    WireContextXmlAdapter adapter = new WireContextXmlAdapter();
    ProtoWireContext proto = new ProtoWireContext("xa-unregistered-tag", "raw-payload");

    JsonNode node = adapter.marshal(new WireContext[] {proto});
    WireContext[] unmarshalled = adapter.unmarshal(node);

    Assert.assertEquals(unmarshalled.length, 1);
    Assert.assertTrue(unmarshalled[0] instanceof ProtoWireContext);
    Assert.assertEquals(((ProtoWireContext)unmarshalled[0]).getSkin(), "xa-unregistered-tag");
  }

  public static class AdapterContext extends WireContext {

    private String value;

    public String getValue () {

      return value;
    }

    public void setValue (String value) {

      this.value = value;
    }
  }
}
