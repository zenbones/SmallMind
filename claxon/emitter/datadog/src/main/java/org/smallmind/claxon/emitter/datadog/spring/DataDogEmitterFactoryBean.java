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
package org.smallmind.claxon.emitter.datadog.spring;

import org.smallmind.claxon.emitter.datadog.DataDogEmitter;
import org.smallmind.claxon.registry.Tag;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring factory bean that creates a singleton {@link DataDogEmitter} with configurable connection and tags.
 */
public class DataDogEmitterFactoryBean implements FactoryBean<DataDogEmitter>, InitializingBean {

  private DataDogEmitter emitter;
  private Tag[] constantTags;
  private String prefix;
  private String hostName = "localhost";
  private boolean countAsCount = true;
  private int port = 8125;

  /**
   * Sets an optional metric prefix.
   *
   * @param prefix prefix to prepend to metric names
   */
  public void setPrefix (String prefix) {

    this.prefix = prefix;
  }

  /**
   * Sets the StatsD host name.
   *
   * @param hostName host name
   */
  public void setHostName (String hostName) {

    this.hostName = hostName;
  }

  /**
   * Sets the StatsD port.
   *
   * @param port port number
   */
  public void setPort (int port) {

    this.port = port;
  }

  /**
   * Controls whether count quantities are emitted as counters instead of gauges.
   *
   * @param countAsCount true to send counts as counters
   */
  public void setCountAsCount (boolean countAsCount) {

    this.countAsCount = countAsCount;
  }

  /**
   * Sets constant tags applied to every emission.
   *
   * @param constantTags constant tags
   */
  public void setConstantTags (Tag[] constantTags) {

    this.constantTags = constantTags;
  }

  /**
   * @return always true; emitter is singleton
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * @return produced object type ({@link DataDogEmitter})
   */
  @Override
  public Class<?> getObjectType () {

    return DataDogEmitter.class;
  }

  /**
   * @return the constructed emitter
   */
  @Override
  public DataDogEmitter getObject () {

    return emitter;
  }

  /**
   * Builds the emitter after properties are set.
   */
  @Override
  public void afterPropertiesSet () {

    emitter = new DataDogEmitter(prefix, hostName, port, countAsCount, constantTags);
  }
}
