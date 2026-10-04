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
module org.smallmind.nutsnbolts {

  requires java.management;

  requires transitive java.compiler;
  requires transitive java.naming;
  requires transitive java.rmi;
  requires transitive java.xml;

  requires static freemarker;
  requires static jakarta.xml.soap;
  requires static org.apache.commons.net;
  requires static org.apache.shiro.core;
  requires static org.apache.shiro.crypto.hash;
  requires static org.apache.shiro.lang;
  requires static org.aspectj.runtime;
  requires static org.bouncycastle.provider;
  requires static org.objectweb.asm;
  requires static org.objectweb.asm.util;
  requires static org.yaml.snakeyaml;
  requires static spring.beans;
  requires static spring.context;
  requires static spring.core;
  requires static spring.web;

  requires static transitive jakarta.activation;
  requires static transitive jakarta.mail;
  requires static transitive jakarta.servlet;
  requires static transitive jakarta.validation;
  requires static transitive jakarta.xml.bind;
  requires static transitive jakarta.xml.ws;
  requires static transitive org.bouncycastle.pkix;

  exports org.smallmind.nutsnbolts.apt;
  exports org.smallmind.nutsnbolts.command;
  exports org.smallmind.nutsnbolts.command.sax;
  exports org.smallmind.nutsnbolts.command.template;
  exports org.smallmind.nutsnbolts.context;
  exports org.smallmind.nutsnbolts.csv;
  exports org.smallmind.nutsnbolts.email;
  exports org.smallmind.nutsnbolts.freemarker;
  exports org.smallmind.nutsnbolts.http;
  exports org.smallmind.nutsnbolts.inject;
  exports org.smallmind.nutsnbolts.io;
  exports org.smallmind.nutsnbolts.json;
  exports org.smallmind.nutsnbolts.lang;
  exports org.smallmind.nutsnbolts.lang.web;
  exports org.smallmind.nutsnbolts.layout;
  exports org.smallmind.nutsnbolts.namespace.shiro.realm;
  exports org.smallmind.nutsnbolts.namespace.shiro.realm.spring;
  exports org.smallmind.nutsnbolts.net;
  exports org.smallmind.nutsnbolts.ntp;
  exports org.smallmind.nutsnbolts.property;
  exports org.smallmind.nutsnbolts.reflection;
  exports org.smallmind.nutsnbolts.reflection.aop;
  exports org.smallmind.nutsnbolts.reflection.bean;
  exports org.smallmind.nutsnbolts.reflection.type;
  exports org.smallmind.nutsnbolts.resource;
  exports org.smallmind.nutsnbolts.retry;
  exports org.smallmind.nutsnbolts.scope;
  exports org.smallmind.nutsnbolts.security;
  exports org.smallmind.nutsnbolts.security.kms;
  exports org.smallmind.nutsnbolts.security.password;
  exports org.smallmind.nutsnbolts.security.spring;
  exports org.smallmind.nutsnbolts.security.x509;
  exports org.smallmind.nutsnbolts.servlet;
  exports org.smallmind.nutsnbolts.soap;
  exports org.smallmind.nutsnbolts.spring;
  exports org.smallmind.nutsnbolts.spring.jmx;
  exports org.smallmind.nutsnbolts.spring.property;
  exports org.smallmind.nutsnbolts.spring.remote;
  exports org.smallmind.nutsnbolts.spring.web;
  exports org.smallmind.nutsnbolts.ssl;
  exports org.smallmind.nutsnbolts.time;
  exports org.smallmind.nutsnbolts.util;
  exports org.smallmind.nutsnbolts.validation;
  exports org.smallmind.nutsnbolts.xml;
  exports org.smallmind.nutsnbolts.xml.sax;
  exports org.smallmind.nutsnbolts.zip;

  opens org.smallmind.nutsnbolts.examples;
  opens org.smallmind.nutsnbolts.json to jakarta.xml.bind;
  opens org.smallmind.nutsnbolts.namespace.shiro.realm.spring to spring.core, spring.beans;
  opens org.smallmind.nutsnbolts.resource to spring.core, spring.beans;
  opens org.smallmind.nutsnbolts.security.spring to spring.core, spring.beans;
  opens org.smallmind.nutsnbolts.spring to spring.core, spring.beans;
  opens org.smallmind.nutsnbolts.spring.jmx to spring.core, spring.beans;
  opens org.smallmind.nutsnbolts.spring.property to spring.core, spring.beans;
  opens org.smallmind.nutsnbolts.spring.remote to spring.core, spring.beans;
  opens org.smallmind.nutsnbolts.spring.web to spring.core, spring.beans;
}
