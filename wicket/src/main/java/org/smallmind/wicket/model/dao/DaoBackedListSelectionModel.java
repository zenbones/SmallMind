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
package org.smallmind.wicket.model.dao;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.orm.ORMDao;

public class DaoBackedListSelectionModel<I extends Serializable & Comparable<I>, D extends Durable<I>> implements Serializable, IModel, IDetachable, IClusterable {

  private transient List<D> selectionList;
  private transient boolean attached = false;

  private ORMDao<I, D> backingDao;
  private List<I> idList;

  public DaoBackedListSelectionModel (ORMDao<I, D> backingDao) {

    this.backingDao = backingDao;

    idList = new LinkedList<I>();
  }

  public synchronized Object getObject () {

    if (!attached) {
      selectionList = new LinkedList<D>();
      for (I id : idList) {
        selectionList.add(backingDao.get(id));
      }

      attached = true;
    }

    return selectionList;
  }

  public synchronized void setObject (Object obj) {

    selectionList = (List<D>)obj;
  }

  public synchronized void detach () {

    if (attached) {
      idList.clear();
      for (D durable : selectionList) {
        idList.add(backingDao.getId(durable));
      }

      attached = false;
      selectionList = null;
    }
  }
}
