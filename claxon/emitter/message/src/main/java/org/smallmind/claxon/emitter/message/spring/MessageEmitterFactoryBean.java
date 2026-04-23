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

import java.util.function.Consumer;
import org.smallmind.claxon.emitter.message.MessageEmitter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} that constructs a singleton {@link MessageEmitter} wired with a
 * configurable {@link Consumer}{@code <String>} message consumer.
 *
 * <p>The consumer is injected via {@link #setMessageConsumer(Consumer)} before the Spring
 * container calls {@link #afterPropertiesSet()}, at which point the emitter is built. A
 * typical use case is to supply a {@link org.smallmind.claxon.emitter.message.ScribeLoggerConsumer}
 * as the consumer so that metric messages are routed to the application's logging framework.
 */
public class MessageEmitterFactoryBean implements FactoryBean<MessageEmitter>, InitializingBean {

  /**
   * The singleton {@link MessageEmitter} produced by this factory; populated during
   * {@link #afterPropertiesSet()}.
   */
  private org.smallmind.claxon.emitter.message.MessageEmitter emitter;

  /**
   * The consumer to which the {@link MessageEmitter} will forward formatted metric strings.
   */
  private Consumer<String> messageConsumer;

  /**
   * Sets the {@link Consumer}{@code <String>} that the produced {@link MessageEmitter} will
   * use to dispatch formatted metric strings.
   *
   * @param messageConsumer the consumer to inject; must not be {@code null}
   */
  public void setMessageConsumer (Consumer<String> messageConsumer) {

    this.messageConsumer = messageConsumer;
  }

  /**
   * Indicates that this factory always returns the same emitter instance.
   *
   * @return {@code true} because the produced {@link MessageEmitter} is a singleton
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the type of object produced by this factory.
   *
   * @return {@link MessageEmitter}{@code .class}
   */
  @Override
  public Class<?> getObjectType () {

    return org.smallmind.claxon.emitter.message.MessageEmitter.class;
  }

  /**
   * Returns the singleton {@link MessageEmitter} built during {@link #afterPropertiesSet()}.
   *
   * @return the configured {@link MessageEmitter}
   */
  @Override
  public org.smallmind.claxon.emitter.message.MessageEmitter getObject () {

    return emitter;
  }

  /**
   * Constructs the {@link MessageEmitter} using the configured message consumer after Spring
   * has finished injecting all values.
   */
  @Override
  public void afterPropertiesSet () {

    emitter = new org.smallmind.claxon.emitter.message.MessageEmitter(messageConsumer);
  }
}
