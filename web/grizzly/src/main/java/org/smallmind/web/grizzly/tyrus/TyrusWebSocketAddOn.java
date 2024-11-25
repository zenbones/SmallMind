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
package org.smallmind.web.grizzly.tyrus;

import java.util.Map;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.tyrus.spi.ServerContainer;
import org.smallmind.web.grizzly.GrizzlyInitializationException;

public class TyrusWebSocketAddOn implements AddOn {

  private final ServerContainer serverContainer;
  private final Map<String, Object> tyrusUpgradeRequestProperties;
  private final String contextPath;

  public TyrusWebSocketAddOn (ServerContainer serverContainer, String contextPath, Map<String, Object> tyrusUpgradeRequestProperties) {

    this.serverContainer = serverContainer;
    this.contextPath = contextPath;
    this.tyrusUpgradeRequestProperties = tyrusUpgradeRequestProperties;
  }

  @Override
  public void setup (NetworkListener networkListener, FilterChainBuilder filterChainBuilder) {

    int httpServerFilterIndex;

    if ((httpServerFilterIndex = filterChainBuilder.indexOfType(HttpServerFilter.class)) < 0) {
      throw new GrizzlyInitializationException("Missing http servlet filter in the available filter chain");
    } else {
      // Insert the WebSocketFilter right before HttpServerFilter
      filterChainBuilder.add(httpServerFilterIndex, new TyrusGrizzlyServerFilter(serverContainer, contextPath, tyrusUpgradeRequestProperties));
    }
  }
}
