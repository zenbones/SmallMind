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
package org.smallmind.spark.tanukisoft.integration.spring;

import org.smallmind.spark.tanukisoft.integration.AbstractWrapperListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@link AbstractWrapperListener} specialization that maps the wrapper's start/stop lifecycle onto a Spring
 * {@link ConfigurableApplicationContext}: startup loads and refreshes the context, shutdown closes it. Concrete
 * subclasses decide how the context is produced.
 */
public abstract class SpringContextWrapperListener extends AbstractWrapperListener {

  private ConfigurableApplicationContext applicationContext;

  /**
   * Factory hook implemented by subclasses to supply the Spring context that the listener should manage.
   *
   * @param args the wrapper-supplied arguments (after timeout stripping); normally interpreted as config locations
   * @return a newly created context, typically unrefreshed
   */
  public abstract ConfigurableApplicationContext loadApplicationContext (String[] args);

  /**
   * Lazily creates the application context on first start and ensures it is refreshed so Spring beans are available.
   *
   * @param args arguments forwarded from the wrapper, passed through to {@link #loadApplicationContext(String[])}
   */
  public void startup (String[] args) {

    if (applicationContext == null) {
      applicationContext = loadApplicationContext(args);
    }
    if (!applicationContext.isActive()) {
      applicationContext.refresh();
    }
  }

  /**
   * Closes the managed application context if one has been created and is still active.
   */
  public void shutdown () {

    if ((applicationContext != null) && applicationContext.isActive()) {
      applicationContext.close();
    }
  }
}
