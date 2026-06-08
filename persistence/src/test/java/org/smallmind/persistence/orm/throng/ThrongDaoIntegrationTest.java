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
package org.smallmind.persistence.orm.throng;

import java.util.List;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import org.smallmind.mongodb.throng.ThrongClient;
import org.smallmind.mongodb.throng.ThrongOptions;
import org.smallmind.mongodb.throng.mapping.annotation.Entity;
import org.smallmind.mongodb.throng.mapping.annotation.Property;
import org.smallmind.mongodb.throng.query.Filter;
import org.smallmind.mongodb.throng.query.Query;
import org.smallmind.mongodb.throng.query.Sort;
import org.smallmind.mongodb.throng.query.Updates;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration test for {@link ThrongDao} against a real MongoDB container started through the docker testbench. It
 * drives the DAO end to end over a genuine {@link ThrongClient}, covering native CRUD ({@code persist},
 * {@code get}/{@code acquire}, {@code delete}), the id-based {@code list}/{@code scroll} surface, {@code size}, and
 * the filter/query helpers built from {@code FilterDetails}/{@code QueryDetails}: {@code countByFilter},
 * {@code findByQuery}, {@code listByQuery}, {@code scrollByQuery}, {@code deleteByFilter}, and {@code updateByFilter}.
 *
 * <p>The session is built with {@code boundaryEnforced = false} so the DAO can be driven directly; MongoDB writes are
 * applied immediately, so no explicit transaction wrapping is required. Each test starts from an empty collection.
 *
 * <p>Requires a running Docker daemon; the {@code mongo:latest} image is pulled on first run.
 */
@Test(groups = "integration")
public class ThrongDaoIntegrationTest extends AbstractGroundwaterTest {

  private static final String CONNECTION_STRING = "mongodb://root:secret@localhost:27017/?authSource=admin";
  private static final String DATABASE_NAME = "throng_dao_it";

  private MongoClient mongoClient;
  private ThrongProxySession proxySession;

  public ThrongDaoIntegrationTest () {

    super(DockerApplication.MONGODB);
  }

  @BeforeClass
  @Override
  public void beforeClass ()
    throws Exception {

    super.beforeClass();

    mongoClient = MongoClients.create(CONNECTION_STRING);

    ThrongClient throngClient = new ThrongClient(mongoClient, DATABASE_NAME, new ThrongOptions(false, false, false), Sprocket.class);

    proxySession = new ThrongProxySession("mongodb", null, new ThrongClientFactory(throngClient), false, false);
  }

  @AfterClass
  @Override
  public void afterClass ()
    throws Exception {

    try {
      if (mongoClient != null) {
        mongoClient.close();
      }
    } finally {
      super.afterClass();
    }
  }

  @BeforeMethod
  public void clearCollection () {

    proxySession.getNativeSession().delete(Sprocket.class, Filter.empty(), new DeleteOptions());
  }

  private SprocketDao dao () {

    return new SprocketDao(proxySession);
  }

  private void persist (SprocketDao dao, Sprocket... sprockets) {

    for (Sprocket sprocket : sprockets) {
      dao.persist(sprocket);
    }
  }

  public void testPersistInsertsAndGetRoundTripsThroughDatabase () {

    SprocketDao dao = dao();

    persist(dao, new Sprocket(1L, "alpha", 5));

    Sprocket fetched = dao.get(1L);

    Assert.assertNotNull(fetched, "a persisted durable should be retrievable by id");
    Assert.assertEquals(fetched.getId(), Long.valueOf(1L));
    Assert.assertEquals(fetched.getName(), "alpha");
  }

  public void testAcquireReadsByIdFromTheClient () {

    SprocketDao dao = dao();

    persist(dao, new Sprocket(2L, "beta", 9));

    Sprocket acquired = dao.acquire(Sprocket.class, 2L);

    Assert.assertNotNull(acquired, "acquire should load the durable straight from the client by its id");
    Assert.assertEquals(acquired.getName(), "beta");
  }

  public void testGetReturnsNullForMissingId () {

    Assert.assertNull(dao().get(999L), "an id with no document should resolve to null");
  }

  public void testDeleteRemovesDurable () {

    SprocketDao dao = dao();

    persist(dao, new Sprocket(3L, "doomed", 1));
    dao.delete(new Sprocket(3L, "doomed", 1));

    Assert.assertNull(dao.get(3L), "a deleted durable should no longer be present");
  }

  public void testListAndSizeCoverEveryDurable () {

    SprocketDao dao = dao();

    persist(dao, new Sprocket(10L, "a", 1), new Sprocket(11L, "b", 2), new Sprocket(12L, "c", 3));

    Assert.assertEquals(dao.list().size(), 3);
    Assert.assertEquals(dao.size(), 3L, "size should count every persisted durable");
    Assert.assertEquals(dao.list(2).size(), 2, "the max-results bound should cap the result count");
  }

  public void testListGreaterThanReturnsHigherIds () {

    SprocketDao dao = dao();

    persist(dao, new Sprocket(10L, "a", 1), new Sprocket(11L, "b", 2), new Sprocket(12L, "c", 3));

    List<Sprocket> higher = dao.list(10L, 10);

    Assert.assertEquals(higher.size(), 2, "only ids strictly greater than the lower bound should be returned");
    for (Sprocket sprocket : higher) {
      Assert.assertTrue(sprocket.getId() > 10L, "every returned id should exceed the lower bound");
    }
  }

  public void testListByIdCollectionReturnsRequestedSubset () {

    SprocketDao dao = dao();

    persist(dao, new Sprocket(20L, "a", 1), new Sprocket(21L, "b", 2), new Sprocket(22L, "c", 3));

    List<Sprocket> subset = dao.list(List.of(20L, 22L));

    Assert.assertEquals(subset.size(), 2, "only the requested ids should be returned");
  }

  public void testScrollVariantsStreamEveryDurable () {

    SprocketDao dao = dao();

    persist(dao, new Sprocket(10L, "a", 1), new Sprocket(11L, "b", 2), new Sprocket(12L, "c", 3));

    Assert.assertEquals(drain(dao.scroll()), 3, "scroll should stream every durable");
    Assert.assertEquals(drain(dao.scroll(2)), 3, "a batch-size hint should not change the total streamed");
    Assert.assertEquals(drain(dao.scrollById(10L, 10)), 2, "scrollById should stream only ids beyond the lower bound");
  }

  public void testCountByFilterCountsMatchingDocuments () {

    SprocketDao dao = dao();

    persist(dao, new Sprocket(1L, "a", 1), new Sprocket(2L, "b", 8), new Sprocket(3L, "c", 9));

    long count = dao.countByFilter(new CountFilterDetails() {

      @Override
      public Filters completeFilter (Filters filters) {

        return filters.and(Filter.where("quantity").gte(5));
      }
    });

    Assert.assertEquals(count, 2L, "only documents at or above the quantity floor should be counted");
  }

  public void testFindByQueryReturnsSingleDurable () {

    SprocketDao dao = dao();

    persist(dao, new Sprocket(1L, "alpha", 5), new Sprocket(2L, "beta", 9));

    Sprocket found = dao.findByQuery(new FindQueryDetails() {

      @Override
      public Query completeQuery (Query query) {

        return query.filter(Filter.where("name").eq("beta"));
      }
    });

    Assert.assertNotNull(found, "the single matching durable should be returned");
    Assert.assertEquals(found.getId(), Long.valueOf(2L));
  }

  public void testListByQueryReturnsMatchingDurables () {

    SprocketDao dao = dao();

    persist(dao, new Sprocket(1L, "a", 1), new Sprocket(2L, "b", 8), new Sprocket(3L, "c", 9));

    List<Sprocket> sprockets = dao.listByQuery(new FindQueryDetails() {

      @Override
      public Query completeQuery (Query query) {

        return query.filter(Filter.where("quantity").gte(5)).sort(Sort.on().asc("_id"));
      }
    });

    Assert.assertEquals(sprockets.size(), 2, "only documents at or above the quantity floor should be listed");
    Assert.assertEquals(sprockets.get(0).getId(), Long.valueOf(2L));
    Assert.assertEquals(sprockets.get(1).getId(), Long.valueOf(3L));
  }

  public void testScrollByQueryStreamsMatchingDurables () {

    SprocketDao dao = dao();

    persist(dao, new Sprocket(1L, "a", 1), new Sprocket(2L, "b", 8), new Sprocket(3L, "c", 9));

    Iterable<Sprocket> scrolled = dao.scrollByQuery(new FindQueryDetails() {

      @Override
      public Query completeQuery (Query query) {

        return query.filter(Filter.where("quantity").gte(5));
      }
    });

    Assert.assertEquals(drain(scrolled), 2, "scrollByQuery should stream only matching durables");
  }

  public void testDeleteByFilterRemovesMatchingDocuments () {

    SprocketDao dao = dao();

    persist(dao, new Sprocket(1L, "doomed", 1), new Sprocket(2L, "doomed", 2), new Sprocket(3L, "spared", 3));

    DeleteResult result = dao.deleteByFilter(new DeleteFilterDetails(new DeleteOptions()) {

      @Override
      public Filters completeFilter (Filters filters) {

        return filters.and(Filter.where("name").eq("doomed"));
      }
    });

    Assert.assertEquals(result.getDeletedCount(), 2L, "both matching documents should be deleted");
    Assert.assertEquals(dao.list().size(), 1, "only the non-matching document should remain");
    Assert.assertNotNull(dao.get(3L), "the spared document should survive");
  }

  public void testUpdateByFilterMutatesMatchingDocument () {

    SprocketDao dao = dao();

    persist(dao, new Sprocket(1L, "before", 5));

    dao.updateByFilter(new UpdateFilterDetails(new UpdateOptions()) {

      @Override
      public Filters completeFilter (Filters filters) {

        return filters.and(Filter.where("_id").eq(1L));
      }

      @Override
      public Updates completeUpdates (Updates updates) {

        return updates.set("name", "after");
      }
    });

    Assert.assertEquals(dao.get(1L).getName(), "after", "the matching document should reflect the update");
  }

  private int drain (Iterable<Sprocket> iterable) {

    int count = 0;

    for (Sprocket ignored : iterable) {
      count++;
    }

    return count;
  }

  @Entity("sprockets")
  public static class Sprocket extends ThrongDurable<Long, Sprocket> {

    @Property
    private String name;
    @Property
    private Integer quantity;

    public Sprocket () {

    }

    public Sprocket (Long id, String name, Integer quantity) {

      setId(id);

      this.name = name;
      this.quantity = quantity;
    }

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }

    public Integer getQuantity () {

      return quantity;
    }

    public void setQuantity (Integer quantity) {

      this.quantity = quantity;
    }
  }

  private static class SprocketDao extends ThrongDao<Long, Sprocket> {

    private SprocketDao (ThrongProxySession proxySession) {

      super(proxySession);
    }
  }
}
