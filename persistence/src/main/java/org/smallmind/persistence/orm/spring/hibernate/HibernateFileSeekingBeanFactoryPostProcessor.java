/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.persistence.orm.spring.hibernate;

import java.net.URL;
import java.util.HashMap;
import org.smallmind.persistence.orm.DataSource;
import org.smallmind.persistence.orm.hibernate.HibernateDao;
import org.smallmind.persistence.spring.ManagedDaoSupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

public class HibernateFileSeekingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

  private static final HashMap<String, HashMap<Class, UrlResource>> HBM_DATA_SOURCE_MAP = new HashMap<String, HashMap<Class, UrlResource>>();
  private static final UrlResource[] NO_RESOURCES = new UrlResource[0];

  public static Resource[] getHibernateResources () {

    return getHibernateResources(null);
  }

  public static Resource[] getHibernateResources (String dataSourceKey) {

    UrlResource[] hbmResources;
    HashMap<Class, UrlResource> hbmResourceMap;

    if ((hbmResourceMap = HBM_DATA_SOURCE_MAP.get(dataSourceKey)) == null) {
      return NO_RESOURCES;
    }

    hbmResources = new UrlResource[hbmResourceMap.size()];
    hbmResourceMap.values().toArray(hbmResources);

    return hbmResources;
  }

  public void postProcessBeanFactory (ConfigurableListableBeanFactory configurableListableBeanFactory)
    throws BeansException {

    Class<?> beanClass;
    Class persistentClass;
    DataSource dataSource;
    HashMap<Class, UrlResource> hbmResourceMap;
    URL hbmURL;
    String dataSourceKey = null;
    String packageRemnant;
    String hbmFileName;
    int lastSlashIndex;

    for (String beanName : configurableListableBeanFactory.getBeanDefinitionNames()) {
      if ((beanClass = configurableListableBeanFactory.getType(beanName)) != null) {
        if (HibernateDao.class.isAssignableFrom(beanClass)) {
          if ((dataSource = beanClass.getAnnotation(DataSource.class)) != null) {
            dataSourceKey = dataSource.value();
          }

          if ((hbmResourceMap = HBM_DATA_SOURCE_MAP.get(dataSourceKey)) == null) {
            HBM_DATA_SOURCE_MAP.put(dataSourceKey, hbmResourceMap = new HashMap<Class, UrlResource>());
          }

          if ((persistentClass = ManagedDaoSupport.findDurableClass(beanClass)) == null) {
            throw new FatalBeanException("No inference of the Durable class for type(" + beanClass.getName() + ") was possible");
          }

          while ((persistentClass != null) && (!hbmResourceMap.containsKey(persistentClass))) {
            packageRemnant = persistentClass.getPackage().getName().replace('.', '/');
            hbmFileName = persistentClass.getSimpleName() + ".hbm.xml";
            do {
              if ((hbmURL = configurableListableBeanFactory.getBeanClassLoader().getResource((packageRemnant.length() > 0) ? packageRemnant + '/' + hbmFileName : hbmFileName)) != null) {
                hbmResourceMap.put(persistentClass, new UrlResource(hbmURL));
                break;
              }

              packageRemnant = packageRemnant.length() > 0 ? packageRemnant.substring(0, (lastSlashIndex = packageRemnant.lastIndexOf('/')) >= 0 ? lastSlashIndex : 0) : null;
            } while (packageRemnant != null);

            persistentClass = persistentClass.getSuperclass();
          }
        }
      }
    }
  }
}