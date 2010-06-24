package org.smallmind.wicket.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.smallmind.data.orm.dao.Dao;
import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

public class DAOBackedListSelectionModel<P, I extends Serializable> implements Serializable, IModel, IDetachable, IClusterable {

   private transient List<P> selectionList;
   private transient boolean attached = false;

   private Dao<P, I> backingDao;
   private List<I> idList;

   public DAOBackedListSelectionModel (Dao<P, I> backingDao) {

      this.backingDao = backingDao;

      idList = new LinkedList<I>();
   }

   public synchronized Object getObject () {

      if (!attached) {
         selectionList = new LinkedList<P>();
         for (I id : idList) {
            selectionList.add(backingDao.get(id));
         }

         attached = true;
      }

      return selectionList;
   }

   public synchronized void setObject (Object obj) {

      selectionList = (List<P>)obj;
   }

   public synchronized void detach () {

      if (attached) {
         idList.clear();
         for (P persistedObject : selectionList) {
            idList.add(backingDao.getId(persistedObject));
         }

         attached = false;
         selectionList = null;
      }
   }
}
