package org.smallmind.persistence.orm.hibernate;

import java.util.Collections;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.criterion.DetachedCriteria;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.persistence.orm.WaterfallORMDao;

public abstract class HibernateDao<D extends Durable<Long>> extends WaterfallORMDao<Long, D> {

   private HibernateProxySession proxySession;

   public HibernateDao (HibernateProxySession proxySession) {

      this(proxySession, null);
   }

   public HibernateDao (HibernateProxySession proxySession, VectoredDao<Long, D> vectoredDao) {

      super(vectoredDao);

      this.proxySession = proxySession;
   }

   public Class<Long> getIdClass () {

      return Long.class;
   }

   public Long getId (D durable) {

      return durable.getId();
   }

   public D get (Long id) {

      return get(getManagedClass(), id);
   }

   public D get (Class<D> durableClass, Long id) {

      D durable;
      VectoredDao<Long, D> nextDao = getNextDao();

      if (nextDao != null) {
         if ((durable = nextDao.get(durableClass, id)) != null) {

            return durable;
         }
      }

      if ((durable = durableClass.cast(proxySession.getSession().get(durableClass, id))) != null) {
         if (nextDao != null) {

            return nextDao.persist(durableClass, durable);
         }

         return durable;
      }

      return null;
   }

   public List<D> list () {

      return Collections.checkedList(proxySession.getSession().createCriteria(getManagedClass()).list(), getManagedClass());
   }

   public Iterable<D> scroll () {

      return new ScrollIterator<D>(proxySession.getSession().createCriteria(getManagedClass()).scroll(ScrollMode.SCROLL_INSENSITIVE), getManagedClass());
   }

   public D persist (D durable) {

      return persist(getManagedClass(), durable);
   }

   public D persist (Class<D> durableClass, D durable) {

      D persistentDurable;
      VectoredDao<Long, D> nextDao = getNextDao();

      persistentDurable = durableClass.cast(proxySession.getSession().merge(durable));
      proxySession.flush();

      if (nextDao != null) {
         return nextDao.persist(durableClass, persistentDurable);
      }

      return persistentDurable;
   }

   public void delete (D durable) {

      delete(getManagedClass(), durable);
   }

   public void delete (Class<D> durableClass, D durable) {

      VectoredDao<Long, D> nextDao = getNextDao();

      proxySession.getSession().delete(durable);
      proxySession.flush();

      if (nextDao != null) {
         nextDao.delete(durableClass, durable);
      }
   }

   public D detach (D object) {

      throw new UnsupportedOperationException("Hibernate has no explicit detached state");
   }

   public int executeWithQuery (QueryDetails queryDetails) {

      return constructQuery(queryDetails).executeUpdate();
   }

   public <T> T findByQuery (Class<T> returnType, QueryDetails queryDetails) {

      return returnType.cast(constructQuery(queryDetails).uniqueResult());
   }

   public D findByQuery (QueryDetails queryDetails) {

      return getManagedClass().cast(constructQuery(queryDetails).uniqueResult());
   }

   public <T> List<T> listByQuery (Class<T> returnType, QueryDetails queryDetails) {

      return Collections.checkedList(constructQuery(queryDetails).list(), returnType);
   }

   public List<D> listByQuery (QueryDetails queryDetails) {

      return Collections.checkedList(constructQuery(queryDetails).list(), getManagedClass());
   }

   public <T> Iterable<T> scrollByQuery (Class<T> returnType, QueryDetails queryDetails) {

      return new ScrollIterator<T>(constructQuery(queryDetails).scroll(ScrollMode.SCROLL_INSENSITIVE), returnType);
   }

   public Iterable<D> scrollByQuery (QueryDetails queryDetails) {

      return new ScrollIterator<D>(constructQuery(queryDetails).scroll(ScrollMode.SCROLL_INSENSITIVE), getManagedClass());
   }

   public <T> T findByCriteria (Class<T> returnType, CriteriaDetails criteriaDetails) {

      return returnType.cast(constructCriteria(criteriaDetails).uniqueResult());
   }

   public D findByCriteria (CriteriaDetails criteriaDetails) {

      return getManagedClass().cast(constructCriteria(criteriaDetails).uniqueResult());
   }

   public <T> List<T> listByCriteria (Class<T> returnType, CriteriaDetails criteriaDetails) {

      return Collections.checkedList(constructCriteria(criteriaDetails).list(), returnType);
   }

   public List<D> listByCriteria (CriteriaDetails criteriaDetails) {

      return Collections.checkedList(constructCriteria(criteriaDetails).list(), getManagedClass());
   }

   public <T> Iterable<T> scrollByCriteria (Class<T> returnType, CriteriaDetails criteriaDetails) {

      return new ScrollIterator<T>(constructCriteria(criteriaDetails).scroll(ScrollMode.SCROLL_INSENSITIVE), returnType);
   }

   public Iterable<D> scrollByCriteria (CriteriaDetails criteriaDetails) {

      return new ScrollIterator<D>(constructCriteria(criteriaDetails).scroll(ScrollMode.SCROLL_INSENSITIVE), getManagedClass());
   }

   private Query constructQuery (QueryDetails queryDetails) {

      return queryDetails.completeQuery(proxySession.getSession().createQuery(queryDetails.getQueryString()).setCacheable(true));
   }

   private Criteria constructCriteria (CriteriaDetails criteriaDetails) {

      return criteriaDetails.completeCriteria(((criteriaDetails.getAlias() == null) ? proxySession.getSession().createCriteria(getManagedClass()) : proxySession.getSession().createCriteria(getManagedClass(), criteriaDetails.getAlias())).setCacheable(true));
   }

   public DetachedCriteria detachCriteria () {

      return DetachedCriteria.forClass(getManagedClass());
   }
}
