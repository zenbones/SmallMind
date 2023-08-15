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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.Set;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.spi.Route;

public class OumuamuaChannel implements Channel {

  private final Route route;

  public OumuamuaChannel (Route route) {

    this.route = route;
  }

  public Route getRoute () {

    return route;
  }

  @Override
  public Set<String> getAttributeNames () {

    return null;
  }

  @Override
  public Object getAttribute (String name) {

    return null;
  }

  @Override
  public void setAttribute (String name, Object value) {

  }

  @Override
  public Object removeAttribute (String name) {

    return null;
  }

  @Override
  public void addListener (Listener listener) {

  }

  @Override
  public void removeListener (Listener listener) {

  }

  @Override
  public boolean isWild () {

    return route.isWild();
  }

  @Override
  public boolean isDeepWild () {

    return route.isDeepWild();
  }

  @Override
  public boolean isMeta () {

    return route.isMeta();
  }

  @Override
  public boolean isService () {

    return route.isService();
  }

  @Override
  public boolean isDeliverable () {

    return route.isDeliverable();
  }

  @Override
  public boolean isPersistent () {

    return false;
  }

  @Override
  public void setPersistent (boolean persistent) {

  }

  @Override
  public boolean isReflecting () {

    return false;
  }

  @Override
  public boolean setReflecting () {

    return false;
  }

  @Override
  public void subscribe (Session session) {

  }

  @Override
  public void unsubscribe (Session session) {

  }

  @Override
  public void deliver (Packet packet) {

  }
}
