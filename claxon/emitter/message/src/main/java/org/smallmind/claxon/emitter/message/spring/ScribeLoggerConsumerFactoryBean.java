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
package org.smallmind.claxon.emitter.message.spring;

import org.smallmind.claxon.emitter.message.ScribeLoggerConsumer;
import org.smallmind.scribe.pen.Level;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring factory bean creating a singleton {@link ScribeLoggerConsumer} with configurable caller and level.
 */
public class ScribeLoggerConsumerFactoryBean implements FactoryBean<ScribeLoggerConsumer>, InitializingBean {

  private ScribeLoggerConsumer scribeLoggerConsumer;
  private Class<?> caller;
  private Level level = Level.DEBUG;

  /**
   * Sets the caller class whose logger will be used.
   *
   * @param caller logger owner class
   */
  public void setCaller (Class<?> caller) {

    this.caller = caller;
  }

  /**
   * Sets the log level.
   *
   * @param level log level
   */
  public void setLevel (Level level) {

    this.level = level;
  }

  /**
   * @return always true; consumer is singleton
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * @return produced object type ({@link ScribeLoggerConsumer})
   */
  @Override
  public Class<?> getObjectType () {

    return ScribeLoggerConsumer.class;
  }

  /**
   * @return the configured consumer
   */
  @Override
  public ScribeLoggerConsumer getObject () {

    return scribeLoggerConsumer;
  }

  /**
   * Instantiates the consumer after properties are set.
   */
  @Override
  public void afterPropertiesSet () {

    scribeLoggerConsumer = new ScribeLoggerConsumer(caller, level);
  }
}
