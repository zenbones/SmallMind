/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class DataDogEmitterFactoryBean implements FactoryBean<DataDogEmitter>, InitializingBean {

  private DataDogEmitter emitter;
  private Tag[] constantTags;
  private String prefix;
  private String hostName = "localhost";
  private boolean countAsCount = true;
  private int port = 8125;

  public void setPrefix (String prefix) {

    this.prefix = prefix;
  }

  public void setHostName (String hostName) {

    this.hostName = hostName;
  }

  public void setPort (int port) {

    this.port = port;
  }

  public void setCountAsCount (boolean countAsCount) {

    this.countAsCount = countAsCount;
  }

  public void setConstantTags (Tag[] constantTags) {

    this.constantTags = constantTags;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return DataDogEmitter.class;
  }

  @Override
  public DataDogEmitter getObject () {

    return emitter;
  }

  @Override
  public void afterPropertiesSet () {

    emitter = new DataDogEmitter(prefix, hostName, port, countAsCount, constantTags);
  }
}
