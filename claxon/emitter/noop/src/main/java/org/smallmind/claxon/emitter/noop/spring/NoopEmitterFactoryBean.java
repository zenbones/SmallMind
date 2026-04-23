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
package org.smallmind.claxon.emitter.noop.spring;

import org.smallmind.claxon.emitter.noop.NoopEmitter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} that produces a singleton {@link NoopEmitter}.
 *
 * <p>This factory bean is provided as a convenience for Spring-managed contexts that require a
 * fully wired emitter bean but do not want any actual metric emission to occur. There are no
 * configurable properties; the emitter is instantiated unconditionally during
 * {@link #afterPropertiesSet()}.
 */
public class NoopEmitterFactoryBean implements FactoryBean<NoopEmitter>, InitializingBean {

  /**
   * The singleton {@link NoopEmitter} produced by this factory; populated during
   * {@link #afterPropertiesSet()}.
   */
  private NoopEmitter emitter;

  /**
   * Indicates that this factory always returns the same emitter instance.
   *
   * @return {@code true} because the produced {@link NoopEmitter} is a singleton
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the type of object produced by this factory.
   *
   * @return {@link NoopEmitter}{@code .class}
   */
  @Override
  public Class<?> getObjectType () {

    return NoopEmitter.class;
  }

  /**
   * Returns the singleton {@link NoopEmitter} built during {@link #afterPropertiesSet()}.
   *
   * @return the {@link NoopEmitter} instance
   */
  @Override
  public NoopEmitter getObject () {

    return emitter;
  }

  /**
   * Instantiates the {@link NoopEmitter} after Spring has finished processing bean
   * definitions.
   */
  @Override
  public void afterPropertiesSet () {

    emitter = new NoopEmitter();
  }
}
