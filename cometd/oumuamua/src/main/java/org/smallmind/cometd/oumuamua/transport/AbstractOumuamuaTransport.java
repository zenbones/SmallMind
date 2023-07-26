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
package org.smallmind.cometd.oumuamua.transport;

import java.util.HashMap;
import java.util.Set;
import org.smallmind.cometd.oumuamua.transport.OumuamuaTransport;

public abstract class AbstractOumuamuaTransport implements OumuamuaTransport {

  private final HashMap<String, Object> optionMap = new HashMap<>();
  private final boolean metaConnectDeliveryOnly;
  private final long timeout;
  private final long interval;
  private final long maxInterval;
  private final long maxLazyTimeout;

  // timeout - long polling response delay milliseconds
  // interval - long polling advised interval milliseconds
  // maxInterval - client timeout milliseconds (< 0 leaves it at container default)
  // maxLazyTimeout - lazy message maximum delay milliseconds
  public AbstractOumuamuaTransport (long timeout, long interval, long maxInterval, long maxLazyTimeout, boolean metaConnectDeliveryOnly) {

    this.timeout = (timeout >= 0) ? timeout : 0;
    this.interval = (interval >= 0) ? interval : 0;
    this.maxInterval = maxInterval;
    this.maxLazyTimeout = (maxLazyTimeout >= 0) ? maxLazyTimeout : 0;
    this.metaConnectDeliveryOnly = metaConnectDeliveryOnly;
  }

  @Override
  public long getTimeout () {

    return timeout;
  }

  @Override
  public long getInterval () {

    return interval;
  }

  @Override
  public long getMaxInterval () {

    return maxInterval;
  }

  @Override
  public long getMaxLazyTimeout () {

    return maxLazyTimeout;
  }

  @Override
  public boolean isMetaConnectDeliveryOnly () {

    return metaConnectDeliveryOnly;
  }

  @Override
  public Object getOption (String name) {

    return optionMap.get(name);
  }

  @Override
  public Set<String> getOptionNames () {

    return optionMap.keySet();
  }

  @Override
  public void setOption (String name, Object value) {

    optionMap.put(name, value);
  }
}
