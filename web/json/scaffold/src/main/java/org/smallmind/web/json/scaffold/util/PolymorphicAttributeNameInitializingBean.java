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
package org.smallmind.web.json.scaffold.util;

import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link InitializingBean} that pushes the configured polymorphic attribute name into
 * {@link PolymorphicAttributeManager} during application context initialization. The name is stored in every
 * per-application registry bound on the refresh thread (the {@code PerApplicationScope} the refresh runs in, the
 * {@code PerApplicationContext} attached to it, or both), so the refresh must carry at least one binding; with
 * neither, {@code afterPropertiesSet} fails with {@code MissingPerApplicationContextException}, which Spring surfaces
 * wrapped in a {@code BeanCreationException}.
 */
public class PolymorphicAttributeNameInitializingBean implements InitializingBean {

  public String polymorphicAttributeName;

  /**
   * Sets the polymorphic attribute name to apply when this bean initializes.
   *
   * @param polymorphicAttributeName desired attribute name
   */
  public void setPolymorphicAttributeName (String polymorphicAttributeName) {

    this.polymorphicAttributeName = polymorphicAttributeName;
  }

  /**
   * Stores the configured attribute name through {@link PolymorphicAttributeManager} in every per-application
   * registry bound on the refresh thread.
   *
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the refresh thread
   * @throws Exception                                                            as declared by {@link InitializingBean}; nothing else is thrown here
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    PolymorphicAttributeManager.setPolymorphicAttributeName(polymorphicAttributeName);
  }
}
