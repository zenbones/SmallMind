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
package org.smallmind.claxon.registry.json;

import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.time.Stint;
import org.testng.Assert;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

@Test(groups = "unit")
public class StintXmlAdapterTest {

  public void testMarshalProducesNodeWithTimeAndTimeUnitFields () {

    JsonNode node = new StintXmlAdapter().marshal(new Stint(7, TimeUnit.SECONDS));

    Assert.assertEquals(node.get("time").longValue(), 7L);
    Assert.assertEquals(node.get("timeUnit").asString(), "SECONDS");
  }

  public void testMarshalOfNullReturnsNull () {

    Assert.assertNull(new StintXmlAdapter().marshal(null));
  }

  public void testUnmarshalReconstructsStint () {

    JsonNode node = JsonNodeFactory.instance.objectNode()
                      .put("time", 5L)
                      .put("timeUnit", "MINUTES");

    Stint stint = new StintXmlAdapter().unmarshal(node);

    Assert.assertEquals(stint.getTime(), 5L);
    Assert.assertEquals(stint.getTimeUnit(), TimeUnit.MINUTES);
  }

  public void testUnmarshalOfNullReturnsNull () {

    Assert.assertNull(new StintXmlAdapter().unmarshal(null));
  }

  public void testRoundTripPreservesValues () {

    StintXmlAdapter adapter = new StintXmlAdapter();

    Stint original = new Stint(250, TimeUnit.MILLISECONDS);
    Stint roundTripped = adapter.unmarshal(adapter.marshal(original));

    Assert.assertEquals(roundTripped.getTime(), original.getTime());
    Assert.assertEquals(roundTripped.getTimeUnit(), original.getTimeUnit());
  }
}
