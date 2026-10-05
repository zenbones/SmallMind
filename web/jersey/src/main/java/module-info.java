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
module org.smallmind.web.jersey {

  requires jakarta.inject;
  requires org.apache.httpcomponents.client5.httpclient5;
  requires org.apache.httpcomponents.core5.httpcore5;
  requires org.glassfish.jersey.core.common;
  requires org.hibernate.validator;
  requires org.smallmind.web.http;
  requires spring.beans;
  requires spring.context;
  requires spring.web;

  requires transitive jakarta.annotation;
  requires transitive jakarta.servlet;
  requires transitive jakarta.validation;
  requires transitive jakarta.ws.rs;
  requires transitive jakarta.xml.bind;
  requires transitive org.glassfish.jersey.core.server;
  requires transitive org.glassfish.jersey.ext.bean.validation;
  requires transitive org.glassfish.jersey.media.multipart;
  requires transitive org.smallmind.nutsnbolts;
  requires transitive org.smallmind.scribe.pen;
  requires transitive org.smallmind.web.json.scaffold;

  requires static org.aspectj.runtime;

  exports org.smallmind.web.jersey.aop;
  exports org.smallmind.web.jersey.cors;
  exports org.smallmind.web.jersey.cors.spring;
  exports org.smallmind.web.jersey.json;
  exports org.smallmind.web.jersey.multipart;
  exports org.smallmind.web.jersey.page;
  exports org.smallmind.web.jersey.proxy;
  exports org.smallmind.web.jersey.proxy.spring;
  exports org.smallmind.web.jersey.spring;
  exports org.smallmind.web.jersey.ssl;

  opens org.smallmind.web.jersey.aop to org.glassfish.hk2.locator, org.glassfish.hk2.utilities, org.glassfish.jersey.core.server, jakarta.xml.bind, tools.jackson.databind;
  opens org.smallmind.web.jersey.cors.spring to spring.core, spring.beans;
  opens org.smallmind.web.jersey.page to org.glassfish.hk2.locator, org.glassfish.hk2.utilities, org.glassfish.jersey.core.server;
  opens org.smallmind.web.jersey.proxy.spring to spring.core, spring.beans;
  opens org.smallmind.web.jersey.spring to spring.core, spring.beans;
}
