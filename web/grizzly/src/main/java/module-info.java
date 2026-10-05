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
module org.smallmind.web.grizzly {

  requires grizzly.http.server.jaxws;
  requires jakarta.xml.bind;
  requires org.glassfish.grizzly.http;
  requires org.glassfish.grizzly.http2;
  requires org.glassfish.grizzly.npn;
  requires org.glassfish.jersey.container.servlet;
  requires org.glassfish.tyrus.container.grizzly.server;
  requires org.smallmind.scribe.pen;
  requires org.smallmind.web.jersey;
  requires spring.beans;
  requires spring.context;
  requires spring.web;

  requires transitive jakarta.servlet;
  requires transitive jakarta.websocket;
  requires transitive jakarta.websocket.client;
  requires transitive org.glassfish.grizzly;
  requires transitive org.glassfish.grizzly.http.server;
  requires transitive org.glassfish.grizzly.servlet;
  requires transitive org.glassfish.jersey.core.server;
  requires transitive org.glassfish.tyrus.core;
  requires transitive org.glassfish.tyrus.server;
  requires transitive org.glassfish.tyrus.spi;
  requires transitive org.smallmind.nutsnbolts;

  exports org.smallmind.web.grizzly;
  exports org.smallmind.web.grizzly.installer;
  exports org.smallmind.web.grizzly.option;
  exports org.smallmind.web.grizzly.tyrus;

  opens org.smallmind.web.grizzly;
}
