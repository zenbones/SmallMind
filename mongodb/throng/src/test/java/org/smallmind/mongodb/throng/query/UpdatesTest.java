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

import com.mongodb.MongoClientSettings;
import org.bson.BsonDocument;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class UpdatesTest {

  public void testSetProducesSetClauseWithCorrectField () {

    BsonDocument doc = (BsonDocument)Updates.of().set("name", "alice").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$set"));
    Assert.assertTrue(doc.getDocument("$set").containsKey("name"));
  }

  public void testUnsetProducesUnsetClause () {

    BsonDocument doc = (BsonDocument)Updates.of().unset("obsolete").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$unset"));
    Assert.assertTrue(doc.getDocument("$unset").containsKey("obsolete"));
  }

  public void testIncProducesIncClause () {

    BsonDocument doc = (BsonDocument)Updates.of().inc("counter", 1).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$inc"));
    Assert.assertTrue(doc.getDocument("$inc").containsKey("counter"));
  }

  public void testMultipleOperationsAccumulateDistinctClauses () {

    BsonDocument doc = (BsonDocument)Updates.of().set("a", 1).unset("b").inc("c", 5)
                                       .toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$set"));
    Assert.assertTrue(doc.containsKey("$unset"));
    Assert.assertTrue(doc.containsKey("$inc"));
  }

  public void testSetOnInsertProducesSetOnInsertClause () {

    BsonDocument doc = (BsonDocument)Updates.of().setOnInsert("createdAt", "now").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$setOnInsert"));
  }

  public void testPushProducesPushClause () {

    BsonDocument doc = (BsonDocument)Updates.of().push("tags", "beta").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$push"));
    Assert.assertTrue(doc.getDocument("$push").containsKey("tags"));
  }

  public void testPullProducesPullClause () {

    BsonDocument doc = (BsonDocument)Updates.of().pull("tags", "stale").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$pull"));
  }

  public void testRenameProducesRenameClause () {

    BsonDocument doc = (BsonDocument)Updates.of().rename("oldName", "newName").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$rename"));
    Assert.assertTrue(doc.getDocument("$rename").containsKey("oldName"));
  }

  public void testMulProducesMulClause () {

    BsonDocument doc = (BsonDocument)Updates.of().mul("price", 2).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$mul"));
    Assert.assertTrue(doc.getDocument("$mul").containsKey("price"));
  }

  public void testMaxProducesMaxClause () {

    BsonDocument doc = (BsonDocument)Updates.of().max("highScore", 999).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$max"));
    Assert.assertTrue(doc.getDocument("$max").containsKey("highScore"));
  }

  public void testMinProducesMinClause () {

    BsonDocument doc = (BsonDocument)Updates.of().min("lowScore", 0).toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$min"));
    Assert.assertTrue(doc.getDocument("$min").containsKey("lowScore"));
  }

  public void testAddToSetProducesAddToSetClause () {

    BsonDocument doc = (BsonDocument)Updates.of().addToSet("roles", "editor").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$addToSet"));
    Assert.assertTrue(doc.getDocument("$addToSet").containsKey("roles"));
  }

  public void testPopFirstProducesPopClauseWithNegativeOne () {

    BsonDocument doc = (BsonDocument)Updates.of().popFirst("items").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$pop"));
    Assert.assertEquals(doc.getDocument("$pop").getInt32("items").getValue(), -1);
  }

  public void testPopLastProducesPopClauseWithOne () {

    BsonDocument doc = (BsonDocument)Updates.of().popLast("items").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertTrue(doc.containsKey("$pop"));
    Assert.assertEquals(doc.getDocument("$pop").getInt32("items").getValue(), 1);
  }
}
