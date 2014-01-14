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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.spring;

import java.util.LinkedList;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.PriorityOrdered;

public class PrivatePropertyResourceBean implements BeanFactoryPostProcessor, PriorityOrdered {

  private static final LinkedList<String> LOCATION_LIST = new LinkedList<>();

  private String location;
  private int order;

  public static String[] getLocations () {

    String[] locations = new String[LOCATION_LIST.size()];

    LOCATION_LIST.toArray(locations);

    return locations;
  }

  @Override
  public int getOrder () {

    return order;
  }

  public void setOrder (int order) {

    this.order = order;
  }

  public void setLocation (String location) {

    this.location = location;
  }

  @Override
  // We exist as a post processor merely to get into the first 'special' initialization phase
  public void postProcessBeanFactory (ConfigurableListableBeanFactory beanFactory) throws BeansException {

    LOCATION_LIST.add(location);
  }
}
