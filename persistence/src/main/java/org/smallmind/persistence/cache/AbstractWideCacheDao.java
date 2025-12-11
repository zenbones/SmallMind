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
package org.smallmind.persistence.cache;

import java.io.Serializable;
import java.util.List;
import org.smallmind.persistence.Durable;

public abstract class AbstractWideCacheDao<W extends Serializable & Comparable<W>, I extends Serializable & Comparable<I>, D extends Durable<I>> implements WideCacheDao<W, I, D> {

  private final CacheDomain<I, D> cacheDomain;

  public AbstractWideCacheDao (CacheDomain<I, D> cacheDomain) {

    this.cacheDomain = cacheDomain;
  }

  @Override
  public String getMetricSource () {

    return cacheDomain.getMetricSource();
  }

  @Override
  public PersistenceCache<String, List<D>> getWideInstanceCache (Class<D> durableClass) {

    return cacheDomain.getWideInstanceCache(durableClass);
  }

  @Override
  public List<D> get (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass) {

    WideDurableKey<W, D> wideDurableKey = new WideDurableKey<W, D>(context, parentClass, parentId, durableClass);

    return getWideInstanceCache(durableClass).get(wideDurableKey.getKey());
  }

  @Override
  public void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass) {

    WideDurableKey<W, D> wideDurableKey = new WideDurableKey<W, D>(context, parentClass, parentId, durableClass);

    getWideInstanceCache(durableClass).remove(wideDurableKey.getKey());
  }
}