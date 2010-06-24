package org.smallmind.quorum.cache.hibernate.spring;

import org.smallmind.quorum.cache.hibernate.HibernateCache;
import org.smallmind.quorum.cache.hibernate.HibernateCacheFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class HibernateCacheFactoryBean implements FactoryBean, InitializingBean, DisposableBean {

   private HibernateCache hibernateCache;
   private HibernateCacheFactory hibernateCacheFactory;
   private String regionName;
   private int limit;
   private int timeToLiveSeconds;

   public void setCacheImplementationProvider (HibernateCacheFactory hibernateCacheFactory) {

      this.hibernateCacheFactory = hibernateCacheFactory;
   }

   public void setRegionName (String regionName) {

      this.regionName = regionName;
   }

   public void setLimit (int limit) {

      this.limit = limit;
   }

   public void setTimeToLiveSeconds (int timeToLiveSeconds) {

      this.timeToLiveSeconds = timeToLiveSeconds;
   }

   public void afterPropertiesSet ()
      throws Exception {

      hibernateCache = new HibernateCache(hibernateCacheFactory, regionName, limit, timeToLiveSeconds);
   }

   public Object getObject ()
      throws Exception {

      return hibernateCache;
   }

   public Class getObjectType () {

      return HibernateCache.class;
   }

   public boolean isSingleton () {

      return true;
   }

   public void destroy ()
      throws Exception {

      hibernateCache.destroy();
   }
}
