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
package org.smallmind.mongodb.throng;

import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class BsonUtilityTest {

  public void testWriteAndReadFlatDocumentPreservesScalarFields () {

    BsonDocument original = new BsonDocument()
                              .append("name", new BsonString("alice"))
                              .append("age", new BsonInt32(30))
                              .append("balance", new BsonInt64(123456789L))
                              .append("active", new BsonBoolean(true));

    BsonDocument result = roundTrip(original);

    Assert.assertEquals(result.getString("name").getValue(), "alice");
    Assert.assertEquals(result.getInt32("age").getValue(), 30);
    Assert.assertEquals(result.getInt64("balance").getValue(), 123456789L);
    Assert.assertTrue(result.getBoolean("active").getValue());
  }

  public void testWriteAndReadDocumentWithNullPreservesBsonNull () {

    BsonDocument original = new BsonDocument().append("missing", BsonNull.VALUE);

    BsonDocument result = roundTrip(original);

    Assert.assertTrue(result.isNull("missing"));
  }

  public void testWriteAndReadNestedDocumentPreservesInnerStructure () {

    BsonDocument inner = new BsonDocument().append("city", new BsonString("Berlin")).append("zip", new BsonInt32(10115));
    BsonDocument original = new BsonDocument().append("name", new BsonString("alice")).append("address", inner);

    BsonDocument result = roundTrip(original);

    Assert.assertEquals(result.getDocument("address").getString("city").getValue(), "Berlin");
    Assert.assertEquals(result.getDocument("address").getInt32("zip").getValue(), 10115);
  }

  public void testWriteAndReadArrayInsideDocumentPreservesMixedElementTypes () {

    BsonArray array = new BsonArray();

    array.add(new BsonInt32(1));
    array.add(new BsonString("two"));
    array.add(BsonNull.VALUE);
    array.add(new BsonDocument("nested", new BsonBoolean(true)));

    BsonDocument original = new BsonDocument().append("items", array);

    BsonDocument result = roundTrip(original);

    BsonArray decoded = result.getArray("items");

    Assert.assertEquals(decoded.size(), 4);
    Assert.assertEquals(((BsonInt32)decoded.get(0)).getValue(), 1);
    Assert.assertEquals(((BsonString)decoded.get(1)).getValue(), "two");
    Assert.assertTrue(decoded.get(2).isNull());
    Assert.assertTrue(((BsonDocument)decoded.get(3)).getBoolean("nested").getValue());
  }

  public void testWriteUnsupportedBsonTypeThrowsDocumentParsingException () {

    BsonDocumentWriter writer = new BsonDocumentWriter(new BsonDocument());

    writer.writeStartDocument();
    writer.writeName("regex");

    Assert.assertThrows(DocumentParsingException.class, () -> BsonUtility.write(writer, new org.bson.BsonRegularExpression("^a")));
  }

  private BsonDocument roundTrip (BsonDocument source) {

    BsonDocument destination = new BsonDocument();
    BsonDocumentWriter writer = new BsonDocumentWriter(destination);

    BsonUtility.write(writer, source);

    BsonDocumentReader reader = new BsonDocumentReader(destination);

    reader.readBsonType();

    BsonValue value = BsonUtility.read(reader);

    return (BsonDocument)value;
  }
}
