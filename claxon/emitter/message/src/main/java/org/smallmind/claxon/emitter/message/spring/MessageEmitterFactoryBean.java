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
 * Spring factory bean producing a singleton {@link MessageEmitter} with a configurable consumer.
 */
public class MessageEmitterFactoryBean implements FactoryBean<MessageEmitter>, InitializingBean {

  private org.smallmind.claxon.emitter.message.MessageEmitter emitter;
  private Consumer<String> messageConsumer;

  /**
   * Sets the consumer that will receive formatted metric messages.
   *
   * @param messageConsumer consumer to use
   */
  public void setMessageConsumer (Consumer<String> messageConsumer) {

    this.messageConsumer = messageConsumer;
  }

  /**
   * @return always true; emitter is a singleton
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * @return produced object type ({@link MessageEmitter})
   */
  @Override
  public Class<?> getObjectType () {

    return org.smallmind.claxon.emitter.message.MessageEmitter.class;
  }

  /**
   * @return the configured message emitter
   */
  @Override
  public org.smallmind.claxon.emitter.message.MessageEmitter getObject () {

    return emitter;
  }

  /**
   * Instantiates the emitter after properties are set.
   */
  @Override
  public void afterPropertiesSet () {

    emitter = new org.smallmind.claxon.emitter.message.MessageEmitter(messageConsumer);
  }
}
