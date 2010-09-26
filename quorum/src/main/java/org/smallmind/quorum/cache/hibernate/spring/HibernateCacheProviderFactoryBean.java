/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
