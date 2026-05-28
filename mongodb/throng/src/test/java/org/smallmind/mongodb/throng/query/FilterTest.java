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
package org.smallmind.mongodb.throng.query;

import java.util.Arrays;
import java.util.regex.Pattern;
import com.mongodb.MongoClientSettings;
import org.bson.BsonDocument;
import org.bson.BsonType;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class FilterTest {

  public void testEmptyFilterProducesValidBson () {

    BsonDocument doc = (BsonDocument)Filter.empty().toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertNotNull(doc);
  }

  public void testWhereEqProducesDocumentContainingField () {

    BsonDocument doc = (BsonDocument)Filter.where("status").eq("active").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("status"));
  }

  public void testSecondComparisonOnClosedFilterThrows () {

    Filter filter = Filter.where("score").gt(0);

    Assert.assertThrows(IllegalFilterStateException.class, () -> filter.lt(100));
  }

  public void testNotOnUninitializedFilterThrows () {

    Filter filter = Filter.where("score");

    Assert.assertThrows(IllegalFilterStateException.class, () -> filter.not());
  }

  public void testToBsonDocumentOnUninitializedFilterThrows () {

    Filter filter = Filter.where("score");

    Assert.assertThrows(UnsupportedOperationException.class, () -> filter.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry()));
  }

  public void testNotNegatesExistingComparison () {

    BsonDocument doc = (BsonDocument)Filter.where("active").eq(true).not().toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertNotNull(doc);
  }

  public void testAndWithNullProducesEmptyFilter () {

    BsonDocument doc = (BsonDocument)Filter.and((Filter[])null).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertNotNull(doc);
  }

  public void testAndWithEmptyArrayProducesEmptyFilter () {

    BsonDocument doc = (BsonDocument)Filter.and().toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertNotNull(doc);
  }

  public void testAndWithIncompleteFilterThrows () {

    Assert.assertThrows(IllegalFilterStateException.class, () -> Filter.and(Filter.where("x")));
  }

  public void testAndWithSingleCompleteFilterProducesValidBson () {

    BsonDocument doc = (BsonDocument)Filter.and(Filter.where("x").eq(1)).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertNotNull(doc);
  }

  public void testAndWithMultipleCompleteFiltersProducesAndClause () {

    BsonDocument doc = (BsonDocument)Filter.and(Filter.where("a").gt(0), Filter.where("b").lt(100))
                                       .toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$and"));
  }

  public void testOrWithNullProducesEmptyFilter () {

    BsonDocument doc = (BsonDocument)Filter.or((Filter[])null).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertNotNull(doc);
  }

  public void testOrWithIncompleteFilterThrows () {

    Assert.assertThrows(IllegalFilterStateException.class, () -> Filter.or(Filter.where("x")));
  }

  public void testOrWithSingleCompleteFilterProducesValidBson () {

    BsonDocument doc = (BsonDocument)Filter.or(Filter.where("x").eq(1)).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertNotNull(doc);
  }

  public void testOrWithMultipleCompleteFiltersProducesOrClause () {

    BsonDocument doc = (BsonDocument)Filter.or(Filter.where("type").eq("A"), Filter.where("type").eq("B"))
                                       .toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$or"));
  }

  public void testOrWithEmptyArrayProducesEmptyFilter () {

    BsonDocument doc = (BsonDocument)Filter.or().toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertNotNull(doc);
  }

  public void testNeProducesNeOperatorInDocument () {

    BsonDocument doc = (BsonDocument)Filter.where("status").ne("inactive").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("status"));
    Assert.assertTrue(doc.getDocument("status").containsKey("$ne"));
  }

  public void testGtProducesGtOperatorInDocument () {

    BsonDocument doc = (BsonDocument)Filter.where("score").gt(50).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("score"));
    Assert.assertTrue(doc.getDocument("score").containsKey("$gt"));
  }

  public void testGteProducesGteOperatorInDocument () {

    BsonDocument doc = (BsonDocument)Filter.where("score").gte(50).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("score"));
    Assert.assertTrue(doc.getDocument("score").containsKey("$gte"));
  }

  public void testLtProducesLtOperatorInDocument () {

    BsonDocument doc = (BsonDocument)Filter.where("score").lt(100).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("score"));
    Assert.assertTrue(doc.getDocument("score").containsKey("$lt"));
  }

  public void testLteProducesLteOperatorInDocument () {

    BsonDocument doc = (BsonDocument)Filter.where("score").lte(100).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("score"));
    Assert.assertTrue(doc.getDocument("score").containsKey("$lte"));
  }

  public void testExistsTrueProducesExistsOperatorWithTrue () {

    BsonDocument doc = (BsonDocument)Filter.where("email").exists(true).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("email"));
    Assert.assertTrue(doc.getDocument("email").getBoolean("$exists").getValue());
  }

  public void testExistsFalseProducesExistsOperatorWithFalse () {

    BsonDocument doc = (BsonDocument)Filter.where("email").exists(false).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("email"));
    Assert.assertFalse(doc.getDocument("email").getBoolean("$exists").getValue());
  }

  public void testInVarargProducesInOperatorInDocument () {

    BsonDocument doc = (BsonDocument)Filter.where("type").in("A", "B", "C").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("type"));
    Assert.assertTrue(doc.getDocument("type").containsKey("$in"));
  }

  public void testInIterableProducesInOperatorInDocument () {

    BsonDocument doc = (BsonDocument)Filter.where("color").in(Arrays.asList("red", "blue")).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("color"));
    Assert.assertTrue(doc.getDocument("color").containsKey("$in"));
  }

  public void testNinProducesNinOperatorInDocument () {

    BsonDocument doc = (BsonDocument)Filter.where("status").nin("deleted", "archived").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("status"));
    Assert.assertTrue(doc.getDocument("status").containsKey("$nin"));
  }

  public void testRegexProducesRegularExpressionValueForField () {

    BsonDocument doc = (BsonDocument)Filter.where("name").regex(Pattern.compile("^alice")).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("name"));
    Assert.assertEquals(doc.get("name").getBsonType(), BsonType.REGULAR_EXPRESSION);
  }

  public void testInWithEmptyIterableProducesValidInWithEmptyArray () {

    BsonDocument doc = (BsonDocument)Filter.where("tags").in(java.util.Collections.emptyList()).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("tags"));
    Assert.assertTrue(doc.getDocument("tags").containsKey("$in"));
    Assert.assertEquals(doc.getDocument("tags").getArray("$in").size(), 0);
  }

  public void testNestedAndOrProducesCompoundFilterStructure () {

    BsonDocument doc = (BsonDocument)Filter.and(
                                        Filter.where("status").eq("OPEN"),
                                        Filter.or(Filter.where("priority").eq("HIGH"), Filter.where("priority").eq("URGENT")))
                                       .toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$and"));
    Assert.assertEquals(doc.getArray("$and").size(), 2);
    Assert.assertTrue(doc.getArray("$and").get(1).asDocument().containsKey("$or"));
  }

  public void testDoubleNotPreservesFilterStructure () {

    BsonDocument doc = (BsonDocument)Filter.where("count").gt(0).not().not().toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertNotNull(doc);
  }
}
