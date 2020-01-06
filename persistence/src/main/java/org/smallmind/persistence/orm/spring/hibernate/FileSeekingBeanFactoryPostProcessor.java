/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.persistence.orm.spring.hibernate;

import java.net.URL;
import java.util.HashMap;
import org.smallmind.persistence.orm.SessionSource;
import org.smallmind.persistence.orm.hibernate.HibernateDao;
import org.smallmind.persistence.ManagedDaoSupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

public class FileSeekingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

  private static final UrlResource[] NO_RESOURCES = new UrlResource[0];
  private final HashMap<String, HashMap<Class, UrlResource>> hbmResourceMap = new HashMap<>();

  public Resource[] getHibernateResources () {

    return getResources(null);
  }

  public Resource[] getResources (String sessionSourceKey) {

    UrlResource[] hbmResources;
    HashMap<Class, UrlResource> hbmResourceMap;

    if ((hbmResourceMap = this.hbmResourceMap.get(sessionSourceKey)) == null) {
      return NO_RESOURCES;
    }

    hbmResources = new UrlResource[hbmResourceMap.size()];
    hbmResourceMap.values().toArray(hbmResources);

    return hbmResources;
  }

  public void postProcessBeanFactory (ConfigurableListableBeanFactory configurableListableBeanFactory)
    throws BeansException {

    for (String beanName : configurableListableBeanFactory.getBeanDefinitionNames()) {

      Class<?> beanClass;

      if ((beanClass = configurableListableBeanFactory.getType(beanName)) != null) {
        if (HibernateDao.class.isAssignableFrom(beanClass)) {

          Class persistentClass;

          if ((persistentClass = ManagedDaoSupport.findDurableClass(beanClass)) == null) {
            throw new FatalBeanException("No inference of the Durable class for type(" + beanClass.getName() + ") was possible");
          } else {

            HashMap<Class, UrlResource> hbmResourceMap;
            SessionSource sessionSource;
            String sessionSourceKey = null;

            if ((sessionSource = beanClass.getAnnotation(SessionSource.class)) != null) {
              sessionSourceKey = sessionSource.value();
            }
            if ((hbmResourceMap = this.hbmResourceMap.get(sessionSourceKey)) == null) {
              this.hbmResourceMap.put(sessionSourceKey, hbmResourceMap = new HashMap<>());
            }

            // Stop when we find a parent class which has already been mapped, signifying the rest of the tree has been previously processed
            while ((persistentClass != null) && (!hbmResourceMap.containsKey(persistentClass))) {

              String packageRemnant;
              String hbmFileName;

              packageRemnant = persistentClass.getPackage().getName().replace('.', '/');
              hbmFileName = persistentClass.getSimpleName() + ".hbm.xml";
              do {

                URL hbmURL;
                int lastSlashIndex;

                if ((hbmURL = configurableListableBeanFactory.getBeanClassLoader().getResource((packageRemnant.length() > 0) ? packageRemnant + '/' + hbmFileName : hbmFileName)) != null) {
                  hbmResourceMap.put(persistentClass, new UrlResource(hbmURL));
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
}