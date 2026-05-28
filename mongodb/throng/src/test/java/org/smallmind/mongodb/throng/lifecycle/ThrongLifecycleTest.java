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
package org.smallmind.mongodb.throng.lifecycle;

import org.bson.BsonDocument;
import org.smallmind.mongodb.throng.ThrongMappingException;
import org.smallmind.mongodb.throng.lifecycle.annotation.PostLoad;
import org.smallmind.mongodb.throng.lifecycle.annotation.PostPersist;
import org.smallmind.mongodb.throng.lifecycle.annotation.PreLoad;
import org.smallmind.mongodb.throng.lifecycle.annotation.PrePersist;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ThrongLifecycleTest {

  public void testPreLoadOnInstanceMethodThrows () {

    Assert.assertThrows(ThrongMappingException.class, () -> new ThrongLifecycle<>(EntityWithInstancePreLoad.class));
  }

  public void testPostLoadOnStaticMethodThrows () {

    Assert.assertThrows(ThrongMappingException.class, () -> new ThrongLifecycle<>(EntityWithStaticPostLoad.class));
  }

  public void testPrePersistOnStaticMethodThrows () {

    Assert.assertThrows(ThrongMappingException.class, () -> new ThrongLifecycle<>(EntityWithStaticPrePersist.class));
  }

  public void testPostPersistOnStaticMethodThrows () {

    Assert.assertThrows(ThrongMappingException.class, () -> new ThrongLifecycle<>(EntityWithStaticPostPersist.class));
  }

  public void testPreLoadCallbackIsDispatched ()
    throws ThrongMappingException {

    ValidEntity.resetPreLoad();

    ThrongLifecycle<ValidEntity> lifecycle = new ThrongLifecycle<>(ValidEntity.class);

    lifecycle.executePreLoad(ValidEntity.class, new BsonDocument());

    Assert.assertTrue(ValidEntity.isPreLoadInvoked());
  }

  public void testPostLoadCallbackIsDispatched ()
    throws ThrongMappingException {

    ValidEntity entity = new ValidEntity();
    ThrongLifecycle<ValidEntity> lifecycle = new ThrongLifecycle<>(ValidEntity.class);

    lifecycle.executePostLoad(entity);

    Assert.assertTrue(entity.isPostLoadInvoked());
  }

  public void testPrePersistCallbackIsDispatched ()
    throws ThrongMappingException {

    ValidEntity entity = new ValidEntity();
    ThrongLifecycle<ValidEntity> lifecycle = new ThrongLifecycle<>(ValidEntity.class);

    lifecycle.executePrePersist(entity);

    Assert.assertTrue(entity.isPrePersistInvoked());
  }

  public void testPostPersistCallbackIsDispatched ()
    throws ThrongMappingException {

    ValidEntity entity = new ValidEntity();
    ThrongLifecycle<ValidEntity> lifecycle = new ThrongLifecycle<>(ValidEntity.class);

    lifecycle.executePostPersist(entity, new BsonDocument());

    Assert.assertTrue(entity.isPostPersistInvoked());
  }

  public static class EntityWithInstancePreLoad {

    @PreLoad
    public void preLoad (BsonDocument doc) {

    }
  }

  public static class EntityWithStaticPostLoad {

    @PostLoad
    public static void postLoad () {

    }
  }

  public static class EntityWithStaticPrePersist {

    @PrePersist
    public static void prePersist () {

    }
  }

  public static class EntityWithStaticPostPersist {

    @PostPersist
    public static void postPersist (BsonDocument doc) {

    }
  }

  public static class ValidEntity {

    private static boolean preLoadInvoked = false;
    private boolean postLoadInvoked = false;
    private boolean prePersistInvoked = false;
    private boolean postPersistInvoked = false;

    @PreLoad
    public static void onPreLoad (BsonDocument doc) {

      preLoadInvoked = true;
    }

    public static boolean isPreLoadInvoked () {

      return preLoadInvoked;
    }

    public static void resetPreLoad () {

      preLoadInvoked = false;
    }

    @PostLoad
    public void onPostLoad () {

      postLoadInvoked = true;
    }

    @PrePersist
    public void onPrePersist () {

      prePersistInvoked = true;
    }

    @PostPersist
    public void onPostPersist (BsonDocument doc) {

      postPersistInvoked = true;
    }

    public boolean isPostLoadInvoked () {

      return postLoadInvoked;
    }

    public boolean isPrePersistInvoked () {

      return prePersistInvoked;
    }

    public boolean isPostPersistInvoked () {

      return postPersistInvoked;
    }
  }
}
