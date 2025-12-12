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

import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.time.Stint;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring factory bean producing a singleton {@link Stint} from configured time and unit.
 */
public class StintFactoryBean implements FactoryBean<Stint>, InitializingBean {

  private Stint stint;
  private TimeUnit timeUnit;
  private long time;

  /**
   * Sets the time value for the stint.
   *
   * @param time duration quantity
   */
  public void setTime (long time) {

    this.time = time;
  }

  /**
   * Sets the time unit for the stint.
   *
   * @param timeUnit duration unit
   */
  public void setTimeUnit (TimeUnit timeUnit) {

    this.timeUnit = timeUnit;
  }

  /**
   * Stints are singletons for a given factory bean configuration.
   *
   * @return always true
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * @return produced object type ({@link Stint})
   */
  @Override
  public Class<?> getObjectType () {

    return Stint.class;
  }

  /**
   * @return the constructed stint
   */
  @Override
  public Stint getObject () {

    return stint;
  }

  /**
   * Builds the stint after Spring injects properties.
   */
  @Override
  public void afterPropertiesSet () {

    stint = new Stint(time, timeUnit);
  }
}
