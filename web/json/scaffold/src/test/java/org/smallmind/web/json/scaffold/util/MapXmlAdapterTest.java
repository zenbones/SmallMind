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
package org.smallmind.web.json.scaffold.util;

import java.util.HashMap;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the two map JAXB adapters: {@link MapXmlAdapter} (map ↔ {@link MapKeyValue} array) and
 * {@link SimpleMapXmlAdapter} (string-keyed map ↔ JSON object with {@code null}-key encoding).
 */
@Test(groups = "unit")
public class MapXmlAdapterTest {

  public void testMapAdapterRoundTrip () {

    PairMapAdapter adapter = new PairMapAdapter();

    HashMap<String, Integer> source = new HashMap<>();
    source.put("a", 1);
    source.put("b", 2);

    MapKeyValue<String, Integer>[] marshalled = adapter.marshal(source);

    Assert.assertEquals(marshalled.length, 2);
    Assert.assertEquals(adapter.unmarshal(marshalled), source);
  }

  public void testMapAdapterNullPassThrough () {

    PairMapAdapter adapter = new PairMapAdapter();

    Assert.assertNull(adapter.marshal(null));
    Assert.assertNull(adapter.unmarshal(null));
  }

  public void testSimpleMapAdapterRoundTripWithNullKey ()
    throws Exception {

    SimpleAdapter adapter = new SimpleAdapter();

    HashMap<String, Integer> source = new HashMap<>();
    source.put("x", 7);
    source.put(null, 9);

    HashMap<String, Integer> recovered = adapter.unmarshal(adapter.marshal(source));

    Assert.assertEquals(recovered.get("x"), Integer.valueOf(7));
    Assert.assertEquals(recovered.get(null), Integer.valueOf(9));
  }

  public void testSimpleMapAdapterEncodesNullKeyAsLiteral ()
    throws Exception {

    SimpleAdapter adapter = new SimpleAdapter();

    HashMap<String, Integer> source = new HashMap<>();
    source.put(null, 9);

    Assert.assertTrue(adapter.marshal(source).has("null"));
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testSimpleMapAdapterRejectsNonObject () {

    new SimpleAdapter().unmarshal(JsonCodec.readAsJsonNode("[1,2,3]"));
  }

  public static class PairMapAdapter extends MapXmlAdapter<HashMap<String, Integer>, String, Integer> {

    @Override
    public HashMap<String, Integer> getEmptyMap () {

      return new HashMap<>();
    }

    @Override
    public Class<String> getKeyClass () {

      return String.class;
    }

    @Override
    public Class<Integer> getValueClass () {

      return Integer.class;
    }
  }

  public static class SimpleAdapter extends SimpleMapXmlAdapter<HashMap<String, Integer>, Integer> {

    @Override
    public HashMap<String, Integer> getEmptyMap () {

      return new HashMap<>();
    }

    @Override
    public Class<Integer> getValueClass () {

      return Integer.class;
    }
  }
}
