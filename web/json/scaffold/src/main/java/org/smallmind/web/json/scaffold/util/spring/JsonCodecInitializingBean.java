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
package org.smallmind.web.json.scaffold.util.spring;

import org.smallmind.web.json.scaffold.util.JsonCodec;
import org.springframework.beans.factory.InitializingBean;
import tools.jackson.databind.JacksonModule;

/**
 * Spring {@link InitializingBean} that extends the shared {@link JsonCodec} with additional Jackson
 * modules. When the bean is initialized, the configured modules are layered on top of the
 * {@link JsonCodec#standardMapperBuilder() standard mapper configuration} and the resulting mapper is
 * installed as the active codec, so every subsequent {@link JsonCodec#instance()} lookup uses the
 * extended configuration.
 */
public class JsonCodecInitializingBean implements InitializingBean {

  private JacksonModule[] additionalModules;

  /**
   * Sets the Jackson modules to register with the {@link JsonCodec} when the bean is initialized.
   *
   * @param additionalModules the modules to add on top of the standard mapper configuration
   */
  public void setAdditionalModules (JacksonModule[] additionalModules) {

    this.additionalModules = additionalModules;
  }

  /**
   * Rebuilds the shared {@link JsonCodec} from the {@link JsonCodec#standardMapperBuilder() standard
   * builder} with the configured modules added and installs it as the active codec.
   *
   * @throws Exception if building the mapper or installing the codec fails
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    JsonCodec.redefine(JsonCodec.standardMapperBuilder().addModules(additionalModules).build());
  }
}
