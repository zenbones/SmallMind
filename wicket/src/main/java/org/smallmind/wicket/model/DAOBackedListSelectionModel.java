package org.smallmind.wicket.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.orm.ORMDao;

public class DAOBackedListSelectionModel<I extends Serializable & Comparable<I>, D extends Durable<I>> implements Serializable, IModel, IDetachable, IClusterable {

   private transient List<D> selectionList;
   private transient boolean attached = false;

   private ORMDao<I, D> backingDao;
   private List<I> idList;

   public DAOBackedListSelectionModel (ORMDao<I, D> backingDao) {

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
