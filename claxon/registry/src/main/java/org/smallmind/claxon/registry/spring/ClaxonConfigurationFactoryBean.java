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
package org.smallmind.claxon.registry.spring;

import java.util.Map;
import org.smallmind.claxon.registry.ClaxonConfiguration;
import org.smallmind.claxon.registry.Clock;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.nutsnbolts.util.DotNotation;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class ClaxonConfigurationFactoryBean implements FactoryBean<ClaxonConfiguration>, InitializingBean {

  private ClaxonConfiguration configuration;
  private Clock clock;
  private Stint collectionStint;
  private Tag[] registryTags;
  private Map<DotNotation, String> prefixMap;

  public void setClock (Clock clock) {

    this.clock = clock;
  }

  public void setCollectionStint (Stint collectionStint) {

    this.collectionStint = collectionStint;
  }

  public void setRegistryTags (Tag[] registryTags) {

    this.registryTags = registryTags;
  }

  public void setPrefixMap (Map<DotNotation, String> prefixMap) {

    this.prefixMap = prefixMap;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return ClaxonConfiguration.class;
  }

  @Override
  public ClaxonConfiguration getObject () {

    return configuration;
  }

  @Override
  public void afterPropertiesSet () throws Exception {

    configuration = new ClaxonConfiguration();
    if (clock != null) {
      configuration.setClock(clock);
    }
    if (collectionStint != null) {
      configuration.setCollectionStint(collectionStint);
    }
    if (registryTags != null) {
      configuration.setRegistryTags(registryTags);
    }
    if (prefixMap != null) {
      configuration.setPrefixMap(prefixMap);
    }
  }
}