package org.smallmind.quorum.cache.hibernate.spring;

import java.util.List;
import org.smallmind.quorum.cache.hibernate.HibernateCache;
import org.smallmind.quorum.cache.hibernate.HibernateCacheProvider;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class HibernateCacheProviderFactoryBean implements FactoryBean, InitializingBean, DisposableBean {

   private HibernateCacheProvider hibernateCacheProvider;
   private List<HibernateCache> cacheList;
   private int limit;
   private int timeToLiveSeconds;

   public HibernateCacheProviderFactoryBean () {

      hibernateCacheProvider = new HibernateCacheProvider();
   }

   public void setLimit (int limit) {

      this.limit = limit;
   }

   public void setTimeToLiveSeconds (int timeToLiveSeconds) {

      this.timeToLiveSeconds = timeToLiveSeconds;
   }

   public void setCacheList (List<HibernateCache> cacheList) {

      this.cacheList = cacheList;
   }

   public void afterPropertiesSet ()
      throws Exception {

      hibernateCacheProvider.setLimit(limit);
      hibernateCacheProvider.setTimeToLiveSeconds(timeToLiveSeconds);

      for (HibernateCache cache : cacheList) {
         hibernateCacheProvider.addCache(cache);
      }
   }

   public Object getObject ()
      throws Exception {

      return hibernateCacheProvider;
   }

   public Class getObjectType () {

      return HibernateCacheProvider.class;
   }

   public boolean isSingleton () {

      return true;
   }

   public void destroy ()
      throws Exception {

      hibernateCacheProvider.stop();
   }
}
