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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.smallmind.mongodb.throng.mapping.annotation.Embedded;
import org.smallmind.mongodb.throng.mapping.annotation.Entity;
import org.smallmind.mongodb.throng.mapping.annotation.Id;
import org.smallmind.mongodb.throng.mapping.annotation.Polymorphic;
import org.smallmind.mongodb.throng.mapping.annotation.Property;
import org.smallmind.mongodb.throng.lifecycle.annotation.PostLoad;
import org.smallmind.mongodb.throng.query.Filter;
import org.smallmind.mongodb.throng.query.Query;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ThrongClientTest {

  private MongoClient mongoClient;

  @BeforeClass
  public void beforeClass () {

    mongoClient = MongoClients.create("mongodb://localhost:27017");
  }

  @AfterClass
  public void afterClass () {

    if (mongoClient != null) {
      mongoClient.close();
    }
  }

  public void testFindOneWithUnmappedEntityThrowsThrongRuntimeException ()
    throws Exception {

    ThrongClient client = new ThrongClient(mongoClient, "throng_unit_test", new ThrongOptions(false, false, false), MappedEntity.class);

    Assert.assertThrows(ThrongRuntimeException.class, () -> client.findOne(UnmappedEntity.class, Query.with().filter(Filter.empty())));
  }

  public void testCountWithUnmappedEntityThrowsThrongRuntimeException ()
    throws Exception {

    ThrongClient client = new ThrongClient(mongoClient, "throng_unit_test", new ThrongOptions(false, false, false), MappedEntity.class);

    Assert.assertThrows(ThrongRuntimeException.class, () -> client.count(UnmappedEntity.class, Filter.empty()));
  }

  public void testConstructionWithNoEntityClassesSucceedsAndAllLookupsFail ()
    throws Exception {

    ThrongClient client = new ThrongClient(mongoClient, "throng_unit_test", new ThrongOptions(false, false, false));

    Assert.assertThrows(ThrongRuntimeException.class, () -> client.count(MappedEntity.class, Filter.empty()));
  }

  public void testEntityWithNoIdFailsWithThrongMappingException () {

    Assert.assertThrows(ThrongMappingException.class, () -> new ThrongClient(mongoClient, "throng_unit_test", new ThrongOptions(false, false, false), EntityWithNoId.class));
  }

  public void testEntityWithMultipleIdsFailsWithThrongMappingException () {

    Assert.assertThrows(ThrongMappingException.class, () -> new ThrongClient(mongoClient, "throng_unit_test", new ThrongOptions(false, false, false), EntityWithTwoIds.class));
  }

  public void testEntityWithPolymorphicFailsWithThrongMappingException () {

    Assert.assertThrows(ThrongMappingException.class, () -> new ThrongClient(mongoClient, "throng_unit_test", new ThrongOptions(false, false, false), EntityWithPolymorphic.class));
  }

  public void testEmbeddedWithLifecycleCallbackFailsWithThrongMappingException () {

    Assert.assertThrows(ThrongMappingException.class, () -> new ThrongClient(mongoClient, "throng_unit_test", new ThrongOptions(false, false, false), EmbeddedWithLifecycle.class));
  }

  @Entity("mapped")
  public static class MappedEntity {

    @Id
    private String id;

    @Property
    private String name;

    public MappedEntity () {

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
  }

  @Entity("unmapped")
  public static class UnmappedEntity {

    @Id
    private String id;

    public UnmappedEntity () {

    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }
  }

  @Entity("no_id")
  public static class EntityWithNoId {

    @Property
    private String name;

    public EntityWithNoId () {

    }

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }
  }

  @Entity("two_ids")
  public static class EntityWithTwoIds {

    @Id
    private String idA;

    @Id("alt")
    private String idB;

    public EntityWithTwoIds () {

    }

    public String getIdA () {

      return idA;
    }

    public void setIdA (String idA) {

      this.idA = idA;
    }

    public String getIdB () {

      return idB;
    }

    public void setIdB (String idB) {

      this.idB = idB;
    }
  }

  @Entity("polymorphic_entity")
  @Polymorphic(value = {EntityWithPolymorphic.class})
  public static class EntityWithPolymorphic {

    @Id
    private String id;

    public EntityWithPolymorphic () {

    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }
  }

  @Embedded
  public static class EmbeddedWithLifecycle {

    @Property
    private String name;

    public EmbeddedWithLifecycle () {

    }

    @PostLoad
    public void afterLoad () {

    }

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }
  }
}
