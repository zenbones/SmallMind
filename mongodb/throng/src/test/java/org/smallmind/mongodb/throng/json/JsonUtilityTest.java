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
package org.smallmind.mongodb.throng.json;

import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.testng.Assert;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@Test(groups = "unit")
public class JsonUtilityTest {

  public void testFromJsonStringNodeProducesBsonString () {

    BsonValue value = JsonUtility.fromJson(JsonNodeFactory.instance.stringNode("alice"));

    Assert.assertEquals(((BsonString)value).getValue(), "alice");
  }

  public void testFromJsonBooleanNodeProducesBsonBoolean () {

    BsonValue value = JsonUtility.fromJson(JsonNodeFactory.instance.booleanNode(true));

    Assert.assertTrue(((BsonBoolean)value).getValue());
  }

  public void testFromJsonNullNodeProducesBsonNull () {

    BsonValue value = JsonUtility.fromJson(JsonNodeFactory.instance.nullNode());

    Assert.assertTrue(value instanceof BsonNull);
  }

  public void testFromJsonIntegerNumberNodeProducesBsonInt32 () {

    BsonValue value = JsonUtility.fromJson(JsonNodeFactory.instance.numberNode(42));

    Assert.assertEquals(((BsonInt32)value).getValue(), 42);
  }

  public void testFromJsonLongNumberNodeProducesBsonInt64 () {

    BsonValue value = JsonUtility.fromJson(JsonNodeFactory.instance.numberNode(9_000_000_000L));

    Assert.assertEquals(((BsonInt64)value).getValue(), 9_000_000_000L);
  }

  public void testFromJsonDoubleNumberNodeProducesBsonDouble () {

    BsonValue value = JsonUtility.fromJson(JsonNodeFactory.instance.numberNode(3.14d));

    Assert.assertEquals(((BsonDouble)value).getValue(), 3.14d);
  }

  public void testFromJsonObjectNodeProducesBsonDocumentWithPreservedFields () {

    ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

    objectNode.put("name", "alice");
    objectNode.put("age", 30);

    BsonValue value = JsonUtility.fromJson(objectNode);

    Assert.assertTrue(value instanceof BsonDocument);
    Assert.assertEquals(((BsonDocument)value).getString("name").getValue(), "alice");
    Assert.assertEquals(((BsonDocument)value).getInt32("age").getValue(), 30);
  }

  public void testFromJsonArrayNodeProducesBsonArrayWithPreservedElements () {

    ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();

    arrayNode.add("a");
    arrayNode.add(1);
    arrayNode.addNull();

    BsonValue value = JsonUtility.fromJson(arrayNode);

    Assert.assertTrue(value instanceof BsonArray);
    Assert.assertEquals(((BsonArray)value).size(), 3);
  }

  public void testToJsonBsonStringProducesTextNode () {

    JsonNode node = JsonUtility.toJson(new BsonString("hello"));

    Assert.assertEquals(node.textValue(), "hello");
  }

  public void testToJsonBsonInt32ProducesIntegerNode () {

    JsonNode node = JsonUtility.toJson(new BsonInt32(7));

    Assert.assertEquals(node.asInt(), 7);
  }

  public void testToJsonBsonInt64ProducesLongNode () {

    JsonNode node = JsonUtility.toJson(new BsonInt64(9_000_000_000L));

    Assert.assertEquals(node.asLong(), 9_000_000_000L);
  }

  public void testToJsonBsonDoubleProducesDoubleNode () {

    JsonNode node = JsonUtility.toJson(new BsonDouble(1.5d));

    Assert.assertEquals(node.asDouble(), 1.5d);
  }

  public void testToJsonBsonBooleanProducesBooleanNode () {

    JsonNode node = JsonUtility.toJson(new BsonBoolean(true));

    Assert.assertTrue(node.booleanValue());
  }

  public void testToJsonBsonNullProducesNullNode () {

    JsonNode node = JsonUtility.toJson(BsonNull.VALUE);

    Assert.assertTrue(node.isNull());
  }

  public void testToJsonBsonDocumentProducesObjectNode () {

    BsonDocument document = new BsonDocument().append("k", new BsonString("v")).append("n", new BsonInt32(2));
    JsonNode node = JsonUtility.toJson(document);

    Assert.assertEquals(node.get("k").textValue(), "v");
    Assert.assertEquals(node.get("n").asInt(), 2);
  }

  public void testToJsonBsonArrayProducesArrayNode () {

    BsonArray array = new BsonArray();

    array.add(new BsonString("x"));
    array.add(new BsonInt32(1));

    JsonNode node = JsonUtility.toJson(array);

    Assert.assertTrue(node.isArray());
    Assert.assertEquals(node.size(), 2);
  }

  public void testRoundTripJsonToBsonToJsonPreservesStructure () {

    ObjectNode original = JsonNodeFactory.instance.objectNode();

    original.put("name", "alice");
    original.set("scores", JsonNodeFactory.instance.arrayNode().add(10).add(20));
    original.set("meta", JsonNodeFactory.instance.objectNode().put("active", true));

    JsonNode result = JsonUtility.toJson(JsonUtility.fromJson(original));

    Assert.assertEquals(result.get("name").textValue(), "alice");
    Assert.assertEquals(result.get("scores").get(0).asInt(), 10);
    Assert.assertTrue(result.get("meta").get("active").booleanValue());
  }
}
