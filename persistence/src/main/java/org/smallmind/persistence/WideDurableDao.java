/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.persistence;

import java.io.Serializable;
import java.util.List;

public interface WideDurableDao<W extends Serializable & Comparable<W>, I extends Serializable & Comparable<I>, D extends Durable<I>> {

  void remove (String context, Class<? extends Durable<W>> parentClass, W parentId);

  void remove (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass);

  List<D> get (String context, Class<? extends Durable<W>> parentClass, W parentId);

  List<D> get (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass);

  List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, D... durables);

  List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, List<D> durables);

  List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, D... durables);

  List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, List<D> durables);

  void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, D... durables);

  void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, List<D> durables);

  void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, D... durables);

  void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, List<D> durables);
}
