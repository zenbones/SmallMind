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
package org.smallmind.quorum.pool.simple;

import org.smallmind.quorum.pool.PoolConfig;

/**
 * Concrete {@link PoolConfig} subclass used to configure the simple {@link ComponentPool}.
 * <p>
 * No additional properties beyond those in {@link PoolConfig} are required for the simple pool.
 * This class exists to close the generic type parameter and provide the correct
 * {@link #getConfigurationClass()} implementation so that the base class's fluent setters return
 * the right concrete type.
 */
public class SimplePoolConfig extends PoolConfig<SimplePoolConfig> {

  /**
   * Creates a configuration with default values inherited from {@link PoolConfig}.
   */
  public SimplePoolConfig () {

  }

  /**
   * Copy constructor that reproduces the pool size and acquire wait time from {@code poolConfig}.
   *
   * @param poolConfig the source configuration to copy
   */
  public SimplePoolConfig (PoolConfig<?> poolConfig) {

    super(poolConfig);
  }

  /**
   * Returns {@link SimplePoolConfig}{@code .class}, enabling the base class to cast fluent
   * setter return values correctly.
   *
   * @return {@code SimplePoolConfig.class}
   */
  @Override
  public Class<SimplePoolConfig> getConfigurationClass () {

    return SimplePoolConfig.class;
  }
}
