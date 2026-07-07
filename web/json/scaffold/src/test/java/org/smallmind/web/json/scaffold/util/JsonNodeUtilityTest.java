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

import org.testng.Assert;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;

/**
 * Exercises {@link JsonNodeUtility#equalsIgnoresOrder} (order-independent structural equality) together
 * with its companion {@link JsonNodeUtility#hashCodeIgnoresOrder}. Beyond the individual behaviours, the
 * {@code assertMatch} helper enforces the equals/hashCode contract that every equal pair must also hash
 * identically — including the tricky {@code 2} vs {@code 2.0} and {@code -0.0} vs {@code 0.0} cases.
 */
@Test(groups = "unit")
public class JsonNodeUtilityTest {

  private static JsonNode node (String json) {

    return JsonCodec.instance().readAsJsonNode(json);
  }

  // Asserts the two documents are order-independent-equal AND (per the contract) hash identically.
  private static void assertMatch (String json1, String json2) {

    JsonNode node1 = node(json1);
    JsonNode node2 = node(json2);

    Assert.assertTrue(JsonNodeUtility.equalsIgnoresOrder(node1, node2), json1 + " ~ " + json2);
    Assert.assertEquals(JsonNodeUtility.hashCodeIgnoresOrder(node1), JsonNodeUtility.hashCodeIgnoresOrder(node2), "hash mismatch: " + json1 + " ~ " + json2);
  }

  private static void assertNoMatch (String json1, String json2) {

    Assert.assertFalse(JsonNodeUtility.equalsIgnoresOrder(node(json1), node(json2)), json1 + " !~ " + json2);
  }

  // --- null and node-type handling ---

  public void testNullReferences () {

    Assert.assertTrue(JsonNodeUtility.equalsIgnoresOrder(null, null));
    Assert.assertFalse(JsonNodeUtility.equalsIgnoresOrder(null, node("1")));
    Assert.assertFalse(JsonNodeUtility.equalsIgnoresOrder(node("1"), null));
  }

  public void testMismatchedNodeTypesAreUnequal () {

    assertNoMatch("{}", "[]");
    assertNoMatch("1", "\"1\"");
    assertNoMatch("true", "1");
  }

  // --- objects: field order ignored ---

  public void testObjectFieldOrderIgnored () {

    assertMatch("{\"a\":1,\"b\":2,\"c\":3}", "{\"c\":3,\"b\":2,\"a\":1}");
  }

  public void testObjectDifferingKeysUnequal () {

    assertNoMatch("{\"a\":1,\"b\":2}", "{\"a\":1,\"c\":2}");
  }

  public void testObjectDifferingValuesUnequal () {

    assertNoMatch("{\"a\":1,\"b\":2}", "{\"a\":1,\"b\":3}");
  }

  public void testObjectDifferentSizeUnequal () {

    assertNoMatch("{\"a\":1}", "{\"a\":1,\"b\":2}");
  }

  // --- arrays: multiset semantics ---

  public void testArrayElementOrderIgnored () {

    assertMatch("[1,2,3]", "[3,1,2]");
  }

  public void testArrayMultiplicityRespected () {

    assertMatch("[1,1,2]", "[2,1,1]");
    assertNoMatch("[1,1,2]", "[1,2,2]");
  }

  public void testArrayDifferentSizeUnequal () {

    assertNoMatch("[1,2]", "[1,2,3]");
  }

  // --- nesting ---

  public void testNestedObjectsAndArraysOrderIgnored () {

    assertMatch("{\"x\":[1,2],\"y\":{\"p\":1,\"q\":2}}", "{\"y\":{\"q\":2,\"p\":1},\"x\":[2,1]}");
  }

  // --- numbers compared as double ---

  public void testIntAndDoubleAreEqual () {

    assertMatch("2", "2.0");
    assertMatch("2", "2.00");
  }

  public void testDifferentNumbersUnequal () {

    assertNoMatch("2", "3");
  }

  // --- scalars ---

  public void testStringsComparedByValue () {

    assertMatch("\"alpha\"", "\"alpha\"");
    assertNoMatch("\"alpha\"", "\"beta\"");
  }

  public void testBooleansComparedByValue () {

    assertMatch("true", "true");
    assertNoMatch("true", "false");
  }

  // --- hashCodeIgnoresOrder: contract specifics ---

  public void testNullHashesToZero () {

    Assert.assertEquals(JsonNodeUtility.hashCodeIgnoresOrder(null), 0);
  }

  public void testPositiveAndNegativeZeroHashAlike () {

    // -0.0 == 0.0 under equalsIgnoresOrder, so the hash must fold them together to honour the contract.
    assertMatch("0.0", "-0.0");
  }

  public void testIntAndDoubleHashAlike () {

    // Reinforces that hashCodeIgnoresOrder normalizes numeric representation, not just equalsIgnoresOrder.
    Assert.assertEquals(JsonNodeUtility.hashCodeIgnoresOrder(node("2")), JsonNodeUtility.hashCodeIgnoresOrder(node("2.0")));
  }
}
