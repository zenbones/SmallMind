/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.cache.praxis.extrinsic;

import java.io.Serializable;
import java.util.List;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.AbstractWideCacheDao;
import org.smallmind.persistence.cache.CacheDomain;
import org.smallmind.persistence.cache.WideDurableKey;

public class WideExtrinsicCacheDao<W extends Serializable & Comparable<W>, I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractWideCacheDao<W, I, D> {

  public WideExtrinsicCacheDao (CacheDomain<I, D> cacheDomain) {

    super(cacheDomain);
  }

  @Override
  public List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, List<D> durables) {

    WideDurableKey<W, D> wideDurableKey = new WideDurableKey<W, D>(context, parentClass, parentId, durableClass);

    getWideInstanceCache(durableClass).set(wideDurableKey.getKey(), durables, 0);

    return durables;
  }
}
