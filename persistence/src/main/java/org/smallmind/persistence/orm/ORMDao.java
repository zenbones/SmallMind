/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.orm;

import java.io.Serializable;
import java.util.List;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.DurableDao;

public interface ORMDao<I extends Serializable & Comparable<I>, D extends Durable<I>, N> extends DurableDao<I, D> {

  public abstract String getDataSource ();

  public abstract ProxySession<N> getSession ();

  public abstract D detach (D durable);

  public abstract D get (I id);

  public abstract D persist (D durable);

  public abstract D persist (Class<D> durableClass, D durable);

  public abstract void delete (D durable);

  public abstract List<D> list ();

  public abstract List<D> list (int fetchSize);

  public abstract List<D> list (I greaterThan, int fetchSize);

  public abstract Iterable<D> scroll ();

  public abstract Iterable<D> scroll (int fetchSize);

  public abstract Iterable<D> scrollById (I greaterThan, int fetchSize);

  public abstract long size ();
}