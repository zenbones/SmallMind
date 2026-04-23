/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.nutsnbolts.spring.jmx;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.PriorityOrdered;

/**
 * A {@link BeanFactoryPostProcessor} that sets the {@code java.rmi.server.hostname} system property early in Spring initialization,
 * using either the local host name or IP address as configured.
 */
public class JMXPropertyInitializingBean implements BeanFactoryPostProcessor, PriorityOrdered {

  private int order;
  private RMIHost rmiHost = RMIHost.HOST_NAME;

  /**
   * Returns the priority order used to sequence this post-processor relative to others.
   *
   * @return the ordering value
   */
  @Override
  public int getOrder () {

    return order;
  }

  /**
   * Sets the priority order used to sequence this post-processor relative to others.
   *
   * @param order the ordering value
   */
  public void setOrder (int order) {

    this.order = order;
  }

  /**
   * Configures whether to use the local host name or IP address when setting the RMI hostname property.
   *
   * @param rmiHost the desired host identifier type; ignored if {@code null}
   */
  public void setRmiHost (RMIHost rmiHost) {

    if (rmiHost != null) {
      this.rmiHost = rmiHost;
    }
  }

  /**
   * Resolves the local host identifier and sets the {@code java.rmi.server.hostname} system property accordingly.
   *
   * @param beanFactory the bean factory provided by Spring (not used directly)
   * @throws FatalBeanException if the local host address cannot be determined
   */
  @Override
  public void postProcessBeanFactory (ConfigurableListableBeanFactory beanFactory)
    throws BeansException {

    try {
      System.setProperty("java.rmi.server.hostname", RMIHost.HOST_NAME.equals(rmiHost) ? InetAddress.getLocalHost().getHostName() : InetAddress.getLocalHost().getHostAddress());
    } catch (UnknownHostException unknownHostException) {
      throw new FatalBeanException(unknownHostException.getMessage(), unknownHostException);
    }
  }
}
