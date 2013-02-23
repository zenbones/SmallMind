/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.cache.memcached.spring;

import java.util.LinkedList;
import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;
import org.smallmind.nutsnbolts.util.Spread;
import org.smallmind.nutsnbolts.util.SpreadParserException;
import org.smallmind.persistence.cache.memcached.MemcachedServer;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class MemcachedServerFactoryBean implements FactoryBean<MemcachedServer[]>, InitializingBean {

  private MemcachedServer[] serverArray;
  private String serverPattern;
  private String serverSpread;

  public void setServerPattern (String serverPattern) {

    this.serverPattern = serverPattern;
  }

  public void setServerSpread (String serverSpread) {

    this.serverSpread = serverSpread;
  }

  @Override
  public void afterPropertiesSet ()
    throws SpreadParserException {

    if ((serverPattern != null) && (serverPattern.length() > 0) && (serverSpread != null) && (serverSpread.length() > 0)) {

      LinkedList<MemcachedServer> serverList;
      int[] serverNumbers = Spread.calculate(serverSpread);
      int poundPos;
      int colonPos;

      if ((poundPos = serverPattern.indexOf('#')) < 0) {
        throw new FormattedRuntimeException("The server pattern(%s) must include a '#' sign", serverPattern);
      }

      colonPos = serverPattern.indexOf(':');
      serverList = new LinkedList<>();
      for (int serverNumber : serverNumbers) {
        if (colonPos >= 0) {
          serverList.add(new MemcachedServer(serverPattern.substring(0, poundPos) + serverNumber + serverPattern.substring(poundPos + 1), Integer.parseInt(serverPattern.substring(colonPos + 1))));
        }
        else {
          serverList.add(new MemcachedServer(serverPattern.substring(0, poundPos) + serverNumber + serverPattern.substring(poundPos + 1), 11211));
        }
      }

      serverArray = new MemcachedServer[serverList.size()];
      serverList.toArray(serverArray);
    }
  }

  @Override
  public MemcachedServer[] getObject () {

    return serverArray;
  }

  @Override
  public Class<?> getObjectType () {

    return MemcachedServer[].class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }
}
