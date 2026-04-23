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
 * Spring {@link FactoryBean} that constructs a singleton {@link ScribeLoggerConsumer} with a
 * configurable caller class and log level.
 *
 * <p>The {@code caller} property identifies the class whose Scribe logger should be used; when
 * left unset it defaults to {@code null} and the consumer falls back to its own class logger.
 * The {@code level} property defaults to {@link Level#DEBUG}. Both may be overridden via
 * standard Spring dependency injection before {@link #afterPropertiesSet()} is called.
 */
public class ScribeLoggerConsumerFactoryBean implements FactoryBean<ScribeLoggerConsumer>, InitializingBean {

  /**
   * The singleton {@link ScribeLoggerConsumer} produced by this factory; populated during
   * {@link #afterPropertiesSet()}.
   */
  private ScribeLoggerConsumer scribeLoggerConsumer;

  /**
   * The class whose Scribe logger will be used by the produced consumer; {@code null} causes
   * the consumer to use its own class logger.
   */
  private Class<?> caller;

  /**
   * The log level at which metric messages will be logged; defaults to {@link Level#DEBUG}.
   */
  private Level level = Level.DEBUG;

  /**
   * Sets the class whose Scribe logger should be used for metric messages.
   *
   * @param caller the logger owner class; may be {@code null} to use the consumer's own class
   */
  public void setCaller (Class<?> caller) {

    this.caller = caller;
  }

  /**
   * Sets the log level at which metric messages will be emitted.
   *
   * @param level the desired {@link Level}; must not be {@code null}
   */
  public void setLevel (Level level) {

    this.level = level;
  }

  /**
   * Indicates that this factory always returns the same consumer instance.
   *
   * @return {@code true} because the produced {@link ScribeLoggerConsumer} is a singleton
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the type of object produced by this factory.
   *
   * @return {@link ScribeLoggerConsumer}{@code .class}
   */
  @Override
  public Class<?> getObjectType () {

    return ScribeLoggerConsumer.class;
  }

  /**
   * Returns the singleton {@link ScribeLoggerConsumer} built during
   * {@link #afterPropertiesSet()}.
   *
   * @return the configured {@link ScribeLoggerConsumer}
   */
  @Override
  public ScribeLoggerConsumer getObject () {

    return scribeLoggerConsumer;
  }

  /**
   * Constructs the {@link ScribeLoggerConsumer} from the configured caller and level after
   * Spring has finished injecting all values.
   */
  @Override
  public void afterPropertiesSet () {

    scribeLoggerConsumer = new ScribeLoggerConsumer(caller, level);
  }
}
