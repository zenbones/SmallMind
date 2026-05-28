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

import java.util.List;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Accumulators;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PipelineTest {

  private static final CodecRegistry REGISTRY = MongoClientSettings.getDefaultCodecRegistry();

  public void testBeginProducesEmptyStageList () {

    Assert.assertTrue(Pipeline.begin().toBsonList().isEmpty());
  }

  public void testMatchStageWrapsFilterInDollarMatch () {

    Pipeline pipeline = Pipeline.begin().match(Filter.where("status").eq("active"));

    BsonDocument stage = renderStage(pipeline, 0);

    Assert.assertTrue(stage.containsKey("$match"));
    Assert.assertTrue(stage.getDocument("$match").containsKey("status"));
  }

  public void testSortStageWrapsSortInDollarSort () {

    Pipeline pipeline = Pipeline.begin().sort(Sort.on().desc("placedAt"));

    BsonDocument stage = renderStage(pipeline, 0);

    Assert.assertTrue(stage.containsKey("$sort"));
    Assert.assertEquals(stage.getDocument("$sort").getInt32("placedAt").getValue(), -1);
  }

  public void testProjectStageWrapsProjectionsInDollarProject () {

    Pipeline pipeline = Pipeline.begin().project(Projections.with().include("name", "score"));

    BsonDocument stage = renderStage(pipeline, 0);

    Assert.assertTrue(stage.containsKey("$project"));
    Assert.assertEquals(stage.getDocument("$project").getInt32("name").getValue(), 1);
    Assert.assertEquals(stage.getDocument("$project").getInt32("score").getValue(), 1);
  }

  public void testLimitStageEmitsDollarLimitWithValue () {

    Pipeline pipeline = Pipeline.begin().limit(25);

    BsonDocument stage = renderStage(pipeline, 0);

    Assert.assertEquals(stage.getInt32("$limit").getValue(), 25);
  }

  public void testSkipStageEmitsDollarSkipWithValue () {

    Pipeline pipeline = Pipeline.begin().skip(10);

    BsonDocument stage = renderStage(pipeline, 0);

    Assert.assertEquals(stage.getInt32("$skip").getValue(), 10);
  }

  public void testCountStageEmitsDollarCountWithFieldName () {

    Pipeline pipeline = Pipeline.begin().count("total");

    BsonDocument stage = renderStage(pipeline, 0);

    Assert.assertEquals(stage.getString("$count").getValue(), "total");
  }

  public void testUnwindStageEmitsDollarUnwindWithPath () {

    Pipeline pipeline = Pipeline.begin().unwind("$tags");

    BsonDocument stage = renderStage(pipeline, 0);

    Assert.assertEquals(stage.getString("$unwind").getValue(), "$tags");
  }

  public void testLookupStageEmitsDollarLookupWithAllFields () {

    Pipeline pipeline = Pipeline.begin().lookup("customers", "customerId", "_id", "customer");

    BsonDocument stage = renderStage(pipeline, 0);

    Assert.assertTrue(stage.containsKey("$lookup"));
    Assert.assertEquals(stage.getDocument("$lookup").getString("from").getValue(), "customers");
    Assert.assertEquals(stage.getDocument("$lookup").getString("localField").getValue(), "customerId");
    Assert.assertEquals(stage.getDocument("$lookup").getString("foreignField").getValue(), "_id");
    Assert.assertEquals(stage.getDocument("$lookup").getString("as").getValue(), "customer");
  }

  public void testGroupStageEmitsDollarGroupWithIdAndAccumulator () {

    Pipeline pipeline = Pipeline.begin().group("$category", Accumulators.sum("total", "$amount"));

    BsonDocument stage = renderStage(pipeline, 0);

    Assert.assertTrue(stage.containsKey("$group"));
    Assert.assertEquals(stage.getDocument("$group").getString("_id").getValue(), "$category");
    Assert.assertTrue(stage.getDocument("$group").containsKey("total"));
  }

  public void testGroupStageWithNullIdGroupsEverythingIntoOneBucket () {

    Pipeline pipeline = Pipeline.begin().group(null, Accumulators.sum("count", 1));

    BsonDocument stage = renderStage(pipeline, 0);

    Assert.assertTrue(stage.containsKey("$group"));
    Assert.assertTrue(stage.getDocument("$group").isNull("_id"));
  }

  public void testRawStageEscapeHatchAddsStageVerbatim () {

    BsonDocument rawStage = new BsonDocument("$addFields", new BsonDocument("derived", new BsonString("constant")));
    Pipeline pipeline = Pipeline.begin().stage(rawStage);

    BsonDocument stage = renderStage(pipeline, 0);

    Assert.assertTrue(stage.containsKey("$addFields"));
    Assert.assertEquals(stage.getDocument("$addFields").getString("derived").getValue(), "constant");
  }

  public void testFluentChainAccumulatesStagesInDeclaredOrder () {

    Pipeline pipeline = Pipeline.begin()
                            .match(Filter.where("status").eq("active"))
                            .sort(Sort.on().desc("placedAt"))
                            .skip(20)
                            .limit(10);

    List<Bson> bsonList = pipeline.toBsonList();

    Assert.assertEquals(bsonList.size(), 4);
    Assert.assertTrue(renderStage(pipeline, 0).containsKey("$match"));
    Assert.assertTrue(renderStage(pipeline, 1).containsKey("$sort"));
    Assert.assertEquals(renderStage(pipeline, 2).getInt32("$skip").getValue(), 20);
    Assert.assertEquals(renderStage(pipeline, 3).getInt32("$limit").getValue(), 10);
  }

  public void testMatchWithIncompleteFilterDefersFailureUntilRender () {

    Pipeline pipeline = Pipeline.begin().match(Filter.where("status"));

    Assert.assertEquals(pipeline.toBsonList().size(), 1);
    Assert.assertThrows(UnsupportedOperationException.class, () -> renderStage(pipeline, 0));
  }

  private static BsonDocument renderStage (Pipeline pipeline, int index) {

    return pipeline.toBsonList().get(index).toBsonDocument(BsonDocument.class, REGISTRY);
  }
}
