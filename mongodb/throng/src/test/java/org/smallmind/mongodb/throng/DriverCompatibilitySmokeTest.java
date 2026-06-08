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

import java.util.List;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.UpdateOptions;
import org.smallmind.testbench.logger.TestLoggerConfiguration;
import org.smallmind.mongodb.throng.index.IndexType;
import org.smallmind.mongodb.throng.index.annotation.Index;
import org.smallmind.mongodb.throng.index.annotation.IndexOptions;
import org.smallmind.mongodb.throng.index.annotation.Indexed;
import org.smallmind.mongodb.throng.index.annotation.Indexes;
import org.smallmind.mongodb.throng.mapping.annotation.Entity;
import org.smallmind.mongodb.throng.mapping.annotation.Id;
import org.smallmind.mongodb.throng.mapping.annotation.Property;
import org.smallmind.mongodb.throng.query.Filter;
import org.smallmind.mongodb.throng.query.Projections;
import org.smallmind.mongodb.throng.query.Query;
import org.smallmind.mongodb.throng.query.Sort;
import org.smallmind.mongodb.throng.query.Updates;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Single end-to-end test exercising every driver-adapter surface in {@code mongodb-throng}: codec registry
 * composition, the full CRUD surface, every query DSL component, automatic index creation, and the streaming
 * BSON read/write path.
 *
 * <p>This test is intended as a fast canary against MongoDB driver upgrades. Run it against any candidate
 * driver version by overriding the parent POM property at the command line:
 *
 * <pre>{@code
 * mvn -pl mongodb/throng -am -Dmongodb.driver.version=5.7.0 -Dmongodb.bson.version=5.7.0 test
 * }</pre>
 *
 * <p>If any driver API touched by Throng changes its signature, behavior, or naming, this test fails first
 * with a focused signal rather than spreading the failure across the rest of the integration suite.
 */
@Test(groups = "integration")
public class DriverCompatibilitySmokeTest extends AbstractGroundwaterTest {

  private MongoClient mongoClient;
  private ThrongClient throngClient;

  public DriverCompatibilitySmokeTest () {

    super(DockerApplication.MONGODB);
  }

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    TestLoggerConfiguration.setup();

    super.beforeClass();

    mongoClient = MongoClients.create("mongodb://root:secret@localhost:27017/?authSource=admin");
    throngClient = new ThrongClient(mongoClient, "throng_smoke_test", new ThrongOptions(false, true, true), SmokeEntity.class);
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    if (mongoClient != null) {
      mongoClient.close();
    }

    super.afterClass();
  }

  public void testEveryDriverAdapterSurfaceRoundTripsAgainstConfiguredDriverVersion () {

    throngClient.delete(SmokeEntity.class, Filter.empty(), new DeleteOptions());

    throngClient.insert(new SmokeEntity("S1", "alpha", 10, "active"), new InsertOneOptions());
    throngClient.insert(new SmokeEntity("S2", "beta", 20, "active"), new InsertOneOptions());
    throngClient.insert(new SmokeEntity("S3", "gamma", 30, "inactive"), new InsertOneOptions());

    Assert.assertEquals(throngClient.count(SmokeEntity.class, Filter.empty()), 3L);
    Assert.assertEquals(throngClient.count(SmokeEntity.class, Filter.where("status").eq("active")), 2L);

    SmokeEntity highestActive = throngClient.findOne(
      SmokeEntity.class,
      Query.with()
        .filter(Filter.where("status").eq("active"))
        .sort(Sort.on().desc("score"))
        .projection(Projections.with().include("name", "score", "status")));

    Assert.assertNotNull(highestActive);
    Assert.assertEquals(highestActive.getName(), "beta");
    Assert.assertEquals(highestActive.getScore(), Integer.valueOf(20));

    List<SmokeEntity> activeAscending = throngClient.find(
      SmokeEntity.class,
      Query.with()
        .filter(Filter.where("status").eq("active"))
        .sort(Sort.on().asc("score"))).asList();

    Assert.assertEquals(activeAscending.size(), 2);
    Assert.assertEquals(activeAscending.get(0).getName(), "alpha");
    Assert.assertEquals(activeAscending.get(1).getName(), "beta");

    UpdateResult updateResult = throngClient.update(
      SmokeEntity.class,
      Filter.where("status").eq("inactive"),
      Updates.of().set("status", "archived").inc("score", 5),
      new UpdateOptions());

    Assert.assertEquals(updateResult.getModifiedCount(), 1L);

    SmokeEntity archived = throngClient.findOne(SmokeEntity.class, Query.with().filter(Filter.where("_id").eq("S3")));

    Assert.assertNotNull(archived);
    Assert.assertEquals(archived.getStatus(), "archived");
    Assert.assertEquals(archived.getScore(), Integer.valueOf(35));

    UpdateResult upsertResult = throngClient.update(
      SmokeEntity.class,
      Filter.where("_id").eq("S4"),
      Updates.of().set("name", "delta").set("score", 40).set("status", "new"),
      new UpdateOptions().upsert(true));

    Assert.assertEquals(upsertResult.getModifiedPlusInsertedCount(), 1L);
    Assert.assertNotNull(upsertResult.getUpsertedId());
    Assert.assertEquals(throngClient.count(SmokeEntity.class, Filter.empty()), 4L);

    throngClient.delete(SmokeEntity.class, Filter.where("status").eq("archived"), new DeleteOptions());

    Assert.assertEquals(throngClient.count(SmokeEntity.class, Filter.empty()), 3L);
  }

  @Entity("smoke_entities")
  @Indexes(
    value = {@Index(value = "status"), @Index(value = "score", type = IndexType.DESCENDING)},
    options = @IndexOptions(name = "status_score_smoke_idx"))
  public static class SmokeEntity {

    @Id
    private String id;

    @Indexed
    @Property
    private String name;

    @Property
    private Integer score;

    @Property
    private String status;

    public SmokeEntity () {

    }

    public SmokeEntity (String id, String name, Integer score, String status) {

      this.id = id;
      this.name = name;
      this.score = score;
      this.status = status;
    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }

    public Integer getScore () {

      return score;
    }

    public void setScore (Integer score) {

      this.score = score;
    }

    public String getStatus () {

      return status;
    }

    public void setStatus (String status) {

      this.status = status;
    }
  }
}
