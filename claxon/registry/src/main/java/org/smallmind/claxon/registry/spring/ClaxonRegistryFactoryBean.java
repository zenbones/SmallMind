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
package org.smallmind.claxon.registry.spring;

import java.util.HashMap;
import java.util.Map;
import org.smallmind.claxon.registry.ClaxonConfiguration;
import org.smallmind.claxon.registry.ClaxonRegistry;
import org.smallmind.claxon.registry.Emitter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring factory bean that wires a {@link ClaxonRegistry} with configuration and emitters.
 */
public class ClaxonRegistryFactoryBean implements FactoryBean<ClaxonRegistry>, InitializingBean, DisposableBean {

  private ClaxonRegistry registry;
  private ClaxonConfiguration configuration = new ClaxonConfiguration();
  private Map<String, Emitter> emitterMap = new HashMap<>();

  /**
   * Sets the registry configuration to use.
   *
   * @param configuration registry configuration
   */
  public void setConfiguration (ClaxonConfiguration configuration) {

    this.configuration = configuration;
  }

  /**
   * Sets the emitters to bind, keyed by name.
   *
   * @param emitterMap emitter map
   */
  public void setEmitterMap (Map<String, Emitter> emitterMap) {

    this.emitterMap = emitterMap;
  }

  /**
   * Registry bean is a singleton.
   *
   * @return always true
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * @return produced object type ({@link ClaxonRegistry})
   */
  @Override
  public Class<?> getObjectType () {

    return ClaxonRegistry.class;
  }

  /**
   * @return the constructed registry
   */
  @Override
  public ClaxonRegistry getObject () {

    return registry;
  }

  /**
   * Stops the registry when the Spring context is shutting down.
   *
   * @throws InterruptedException if interrupted during shutdown
   */
  @Override
  public void destroy ()
    throws InterruptedException {

    if (registry != null) {
      registry.stop();
    }
  }

  /**
   * Builds and initializes the registry after properties are set.
   */
  @Override
  public void afterPropertiesSet () {

    registry = new ClaxonRegistry(configuration);

    for (Map.Entry<String, Emitter> emitterEntry : emitterMap.entrySet()) {
      registry.bind(emitterEntry.getKey(), emitterEntry.getValue());
    }

    registry.initializeInstrumentation();
  }
}
