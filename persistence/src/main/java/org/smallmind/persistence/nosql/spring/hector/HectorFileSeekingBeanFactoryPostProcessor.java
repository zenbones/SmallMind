/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.nosql.spring.hector;

import java.util.HashMap;
import java.util.HashSet;
import org.smallmind.persistence.NaturalKeys;
import org.smallmind.persistence.nosql.hector.HectorDao;
import org.smallmind.persistence.orm.SessionSource;
import org.smallmind.persistence.spring.ManagedDaoSupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class HectorFileSeekingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

  private static final HashMap<String, HashSet<Class>> HECTOR_DATA_SOURCE_MAP = new HashMap<String, HashSet<Class>>();

  public static HashSet<Class> getHectorTypes (String keyspaceName) {

    return HECTOR_DATA_SOURCE_MAP.get(keyspaceName);
  }

  public void postProcessBeanFactory (ConfigurableListableBeanFactory configurableListableBeanFactory)
    throws BeansException {

    Class<?> beanClass;
    Class persistentClass;
    HashSet<Class> hectorTypes;
    SessionSource sessionSource;
    String sessionSourceKey = null;

    for (String beanName : configurableListableBeanFactory.getBeanDefinitionNames()) {
      if ((beanClass = configurableListableBeanFactory.getType(beanName)) != null) {
        if (HectorDao.class.isAssignableFrom(beanClass)) {
          if ((sessionSource = beanClass.getAnnotation(SessionSource.class)) != null) {
            sessionSourceKey = sessionSource.value();
          }

          if ((hectorTypes = HECTOR_DATA_SOURCE_MAP.get(sessionSourceKey)) == null) {
            HECTOR_DATA_SOURCE_MAP.put(sessionSourceKey, hectorTypes = new HashSet<Class>());
          }

          if ((persistentClass = ManagedDaoSupport.findDurableClass(beanClass)) == null) {
            throw new FatalBeanException("No inference of the Durable class for type(" + beanClass.getName() + ") was possible");
          }
          if (persistentClass.getAnnotation(NaturalKeys.class) == null) {
            throw new FatalBeanException("Missing annotation(" + NaturalKeys.class.getSimpleName() + ") on durable type(" + persistentClass.getSimpleName() + ")");
          }

          hectorTypes.add(persistentClass);
        }
      }
    }
  }
}
