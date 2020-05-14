/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.claxon.collector.datadog.spring;

import org.smallmind.claxon.collector.datadog.DataDogCollector;
import org.smallmind.claxon.registry.Tag;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class DataDogCollectorFactoryBean implements FactoryBean<DataDogCollector>, InitializingBean {

  private DataDogCollector collector;

  private String prefix;
  private String hostName = "localhost";
  private int port = 8125;
  private Tag[] constantTags;

  public void setPrefix (String prefix) {

    this.prefix = prefix;
  }

  public void setHostName (String hostName) {

    this.hostName = hostName;
  }

  public void setPort (int port) {

    this.port = port;
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

    return DataDogCollector.class;
  }

  @Override
  public DataDogCollector getObject () {

    return collector;
  }

  @Override
  public void afterPropertiesSet () {

    collector = new DataDogCollector(prefix, hostName, port, constantTags);
  }
}
