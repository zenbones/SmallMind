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
import java.util.concurrent.TimeoutException;
import org.smallmind.claxon.registry.ClaxonConfiguration;
import org.smallmind.claxon.registry.ClaxonRegistry;
import org.smallmind.claxon.registry.Emitter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} that constructs, configures, and manages the lifecycle of a
 * {@link ClaxonRegistry} singleton. During {@link #afterPropertiesSet()} the registry is
 * built from the supplied {@link ClaxonConfiguration}, all configured {@link Emitter}
 * instances are bound by name, and instrumentation is initialized. During
 * {@link #destroy()} the registry is gracefully stopped, making this bean safe for use in
 * Spring application contexts that manage shutdown hooks.
 */
public class ClaxonRegistryFactoryBean implements FactoryBean<ClaxonRegistry>, InitializingBean, DisposableBean {

  /**
   * The registry instance produced and managed by this factory bean.
   */
  private ClaxonRegistry registry;

  /**
   * Registry configuration applied when the registry is constructed; defaults to an empty configuration.
   */
  private ClaxonConfiguration configuration = new ClaxonConfiguration();

  /**
   * Map of emitter name to {@link Emitter} instances that will be bound to the registry.
   */
  private Map<String, Emitter> emitterMap = new HashMap<>();

  /**
   * Sets the {@link ClaxonConfiguration} to apply when constructing the registry.
   *
   * @param configuration the registry configuration; must not be {@code null}
   */
  public void setConfiguration (ClaxonConfiguration configuration) {

    this.configuration = configuration;
  }

  /**
   * Sets the map of named {@link Emitter} instances to bind to the registry. The map key
   * is the emitter name used to identify it within the registry.
   *
   * @param emitterMap a map of emitter name to {@link Emitter} instance; must not be {@code null}
   */
  public void setEmitterMap (Map<String, Emitter> emitterMap) {

    this.emitterMap = emitterMap;
  }

  /**
   * Indicates that the produced {@link ClaxonRegistry} is a singleton within the Spring context.
   *
   * @return {@code true} always
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the type of object produced by this factory bean.
   *
   * @return {@link ClaxonRegistry}{@code .class}
   */
  @Override
  public Class<?> getObjectType () {

    return ClaxonRegistry.class;
  }

  /**
   * Returns the {@link ClaxonRegistry} singleton constructed during {@link #afterPropertiesSet()}.
   *
   * @return the constructed registry, or {@code null} if {@link #afterPropertiesSet()} has not yet run
   */
  @Override
  public ClaxonRegistry getObject () {

    return registry;
  }

  /**
   * Stops the managed {@link ClaxonRegistry} when the Spring context is shutting down,
   * allowing in-flight metric flushes to complete.
   *
   * @throws InterruptedException if the current thread is interrupted while waiting for the
   *                              registry to stop
   */
  @Override
  public void destroy ()
    throws InterruptedException, TimeoutException {

    if (registry != null) {
      registry.stop();
    }
  }

  /**
   * Constructs the {@link ClaxonRegistry} from the configured {@link ClaxonConfiguration},
   * binds all entries from the emitter map, and then calls
   * {@link ClaxonRegistry#initializeInstrumentation()} to activate metric collection.
   * Invoked automatically by the Spring container after all bean properties have been set.
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
