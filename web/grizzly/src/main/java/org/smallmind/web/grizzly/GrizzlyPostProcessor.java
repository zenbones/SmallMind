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
package org.smallmind.web.grizzly;

import java.util.LinkedList;
import org.smallmind.web.grizzly.installer.FilterInstaller;
import org.smallmind.web.grizzly.installer.ListenerInstaller;
import org.smallmind.web.grizzly.installer.ServletInstaller;
import org.smallmind.web.grizzly.installer.WebServiceInstaller;
import org.smallmind.web.grizzly.installer.WebSocketExtensionInstaller;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Bean post-processor that collects installer beans created by Spring and attaches them to their corresponding
 * {@link GrizzlyWebAppState}. Beans created before the locator is available are queued and processed once the locator
 * is discovered.
 */
public class GrizzlyPostProcessor implements BeanPostProcessor {

  private final LinkedList<Object> unprocessedBeans = new LinkedList<>();
  private GrizzlyWebAppStateLocator locator;

  /**
   * Routes installer beans to the appropriate web application state or creates a {@link WebServiceInstaller} for
   * classes annotated with {@link ServicePath}.
   *
   * @param bean the bean to process
   */
  private void processBean (Object bean) {

    ServicePath servicePath;

    if (bean instanceof WebSocketExtensionInstaller) {
      locator.webAppStateFor(((WebSocketExtensionInstaller)bean).getContextPath()).addWebSocketExtensionInstaller((WebSocketExtensionInstaller)bean);
    } else if (bean instanceof ListenerInstaller) {
      locator.webAppStateFor(((ListenerInstaller)bean).getContextPath()).addListenerInstaller((ListenerInstaller)bean);
    } else if (bean instanceof FilterInstaller) {
      locator.webAppStateFor(((FilterInstaller)bean).getContextPath()).addFilterInstaller((FilterInstaller)bean);
    } else if (bean instanceof ServletInstaller) {
      locator.webAppStateFor(((ServletInstaller)bean).getContextPath()).addServletInstaller((ServletInstaller)bean);
    } else if ((servicePath = bean.getClass().getAnnotation(ServicePath.class)) != null) {
      locator.webAppStateFor(servicePath.contextPath()).addWebServiceInstaller(new WebServiceInstaller(servicePath.value(), bean));
    }
  }

  /**
   * Captures the locator when it becomes available and replays any queued beans; otherwise defers processing until the
   * locator is known.
   *
   * @param bean     newly initialized bean
   * @param beanName Spring bean name
   * @return the original bean
   */
  @Override
  public synchronized Object postProcessAfterInitialization (Object bean, String beanName) {

    if (bean instanceof GrizzlyWebAppStateLocator) {
      locator = (GrizzlyWebAppStateLocator)bean;
    } else if (locator == null) {
      if (bean instanceof WebSocketExtensionInstaller) {
        unprocessedBeans.add(bean);
      } else if (bean instanceof ListenerInstaller) {
        unprocessedBeans.add(bean);
      } else if (bean instanceof FilterInstaller) {
        unprocessedBeans.add(bean);
      } else if (bean instanceof ServletInstaller) {
        unprocessedBeans.add(bean);
      } else if (bean.getClass().getAnnotation(ServicePath.class) != null) {
        unprocessedBeans.add(bean);
      }
    } else {
      for (Object unprocessedBean : unprocessedBeans) {
        processBean(unprocessedBean);
      }

      unprocessedBeans.clear();

      processBean(bean);
    }

    return bean;
  }
}
