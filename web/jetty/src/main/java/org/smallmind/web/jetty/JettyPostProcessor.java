/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.web.jetty;

import java.util.LinkedList;
import org.smallmind.web.jetty.installer.FilterInstaller;
import org.smallmind.web.jetty.installer.ListenerInstaller;
import org.smallmind.web.jetty.installer.ServletInstaller;
import org.smallmind.web.jetty.installer.WebServiceInstaller;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class JettyPostProcessor implements BeanPostProcessor {

  private final LinkedList<Object> unprocessedBeans = new LinkedList<>();
  private JettyWebAppStateLocator locator;

  private void processBean (Object bean) {

    ServicePath servicePath;

    if (bean instanceof ListenerInstaller) {
      locator.webAppStateFor(((ListenerInstaller)bean).getContextPath()).addListenerInstaller((ListenerInstaller)bean);
    } else if (bean instanceof FilterInstaller) {
      locator.webAppStateFor(((FilterInstaller)bean).getContextPath()).addFilterInstaller((FilterInstaller)bean);
    } else if (bean instanceof ServletInstaller) {
      locator.webAppStateFor(((ServletInstaller)bean).getContextPath()).addServletInstaller((ServletInstaller)bean);
    } else if ((servicePath = bean.getClass().getAnnotation(ServicePath.class)) != null) {
      locator.webAppStateFor(servicePath.context()).addWebServiceInstaller(new WebServiceInstaller(servicePath.value(), bean));
    }
  }

  @Override
  public synchronized Object postProcessAfterInitialization (Object bean, String beanName) {

    if (bean instanceof JettyWebAppStateLocator) {
      locator = (JettyWebAppStateLocator)bean;
    } else if (locator == null) {
      if (bean instanceof ListenerInstaller) {
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
