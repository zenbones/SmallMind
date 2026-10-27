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
package org.smallmind.phalanx.wire.spring;

import org.smallmind.phalanx.wire.WireScopeManager;
import org.smallmind.phalanx.wire.signal.WireScope;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link InitializingBean} that registers a {@link WireScope} implementation class with the
 * {@link WireScopeManager} under a configured handle during Spring context startup. This is the scope twin of
 * {@link WireContextInitializingBean}: the Spring refresh must run inside the application's
 * {@code PerApplicationScope} (for example under {@code PerApplicationScopeContextLoaderListener} or the Tanuki
 * wrapper), otherwise {@code afterPropertiesSet} fails with {@code MissingPerApplicationScopeException}, which Spring
 * surfaces from the refresh wrapped in a {@code BeanCreationException}.
 */
public class WireScopeInitializingBean implements InitializingBean {

  private Class<? extends WireScope> scopeClass;
  private String handle;

  /**
   * Sets the handle under which the scope class is registered.
   *
   * @param handle unique registration key for the scope
   */
  public void setHandle (String handle) {

    this.handle = handle;
  }

  /**
   * Sets the concrete {@link WireScope} implementation class to register.
   *
   * @param scopeClass scope implementation to associate with the handle
   */
  public void setScopeClass (Class<? extends WireScope> scopeClass) {

    this.scopeClass = scopeClass;
  }

  /**
   * Registers the configured scope class with {@link WireScopeManager} using the configured handle.
   *
   * @throws Exception if registration fails
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    WireScopeManager.register(handle, scopeClass);
  }
}
