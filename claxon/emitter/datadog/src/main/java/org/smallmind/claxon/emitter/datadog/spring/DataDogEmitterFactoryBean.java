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
 * Spring {@link FactoryBean} that constructs a singleton {@link DataDogEmitter} from
 * Spring-injectable configuration properties.
 *
 * <p>Default values mirror those of the no-argument {@link DataDogEmitter} constructor:
 * {@code hostName} defaults to {@code "localhost"}, {@code port} defaults to {@code 8125},
 * and {@code countAsCount} defaults to {@code true}. All properties may be overridden via
 * standard Spring dependency injection before {@link #afterPropertiesSet()} is called.
 */
public class DataDogEmitterFactoryBean implements FactoryBean<DataDogEmitter>, InitializingBean {

  /**
   * The singleton {@link DataDogEmitter} constructed during {@link #afterPropertiesSet()}.
   */
  private DataDogEmitter emitter;

  /**
   * Constant tags applied to every metric emission.
   */
  private Tag[] constantTags;

  /**
   * Optional metric name prefix prepended to every emitted metric name.
   */
  private String prefix;

  /**
   * Hostname of the Datadog StatsD agent; defaults to {@code "localhost"}.
   */
  private String hostName = "localhost";

  /**
   * When {@code true}, quantities of type COUNT are emitted as StatsD counters rather than
   * gauges; defaults to {@code true}.
   */
  private boolean countAsCount = true;

  /**
   * UDP port of the Datadog StatsD agent; defaults to {@code 8125}.
   */
  private int port = 8125;

  /**
   * Sets an optional prefix that is prepended to every metric name before emission.
   *
   * @param prefix the prefix string; may be {@code null} to disable prefixing
   */
  public void setPrefix (String prefix) {

    this.prefix = prefix;
  }

  /**
   * Sets the hostname of the Datadog StatsD agent.
   *
   * @param hostName the StatsD agent hostname; must not be {@code null}
   */
  public void setHostName (String hostName) {

    this.hostName = hostName;
  }

  /**
   * Sets the UDP port on which the Datadog StatsD agent listens.
   *
   * @param port the port number
   */
  public void setPort (int port) {

    this.port = port;
  }

  /**
   * Controls whether quantities of type COUNT are sent as StatsD counters instead of gauges.
   *
   * @param countAsCount {@code true} to emit counts as counters; {@code false} to emit them
   *                     as gauges
   */
  public void setCountAsCount (boolean countAsCount) {

    this.countAsCount = countAsCount;
  }

  /**
   * Sets the constant tags that will be attached to every metric emitted by the produced
   * {@link DataDogEmitter}.
   *
   * @param constantTags array of tags; may be {@code null} or empty
   */
  public void setConstantTags (Tag[] constantTags) {

    this.constantTags = constantTags;
  }

  /**
   * Indicates that this factory always returns the same emitter instance.
   *
   * @return {@code true} because the produced {@link DataDogEmitter} is a singleton
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the type of object produced by this factory.
   *
   * @return {@link DataDogEmitter}{@code .class}
   */
  @Override
  public Class<?> getObjectType () {

    return DataDogEmitter.class;
  }

  /**
   * Returns the singleton {@link DataDogEmitter} built during {@link #afterPropertiesSet()}.
   *
   * @return the configured {@link DataDogEmitter}
   */
  @Override
  public DataDogEmitter getObject () {

    return emitter;
  }

  /**
   * Constructs the {@link DataDogEmitter} from the configured properties after Spring has
   * finished injecting all values.
   */
  @Override
  public void afterPropertiesSet () {

    emitter = new DataDogEmitter(prefix, hostName, port, countAsCount, constantTags);
  }
}
