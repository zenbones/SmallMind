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
module org.smallmind.persistence {

  requires transitive java.logging;
  requires transitive java.sql;
  requires transitive java.transaction.xa;
  requires transitive org.smallmind.nutsnbolts;
  requires transitive org.smallmind.scribe.pen;

  requires static com.querydsl.core;
  requires static com.querydsl.jpa;
  requires static jakarta.persistence;
  requires static org.apache.commons.logging;
  requires static org.aspectj.runtime;
  requires static org.hibernate.orm.core;
  requires static org.mongodb.bson;
  requires static org.mongodb.driver.core;
  requires static org.mongodb.driver.sync.client;
  requires static org.smallmind.claxon.registry;
  requires static org.smallmind.memcached.utility;
  requires static org.smallmind.mongodb.throng;
  requires static org.smallmind.quorum;
  requires static org.smallmind.testbench.condition;
  requires static spring.beans;
  requires static spring.context;
  requires static spring.core;
  requires static spring.orm;

  exports org.smallmind.persistence;
  exports org.smallmind.persistence.cache;
  exports org.smallmind.persistence.cache.aop;
  exports org.smallmind.persistence.cache.memcached;
  exports org.smallmind.persistence.cache.memcached.spring;
  exports org.smallmind.persistence.cache.praxis;
  exports org.smallmind.persistence.cache.praxis.extrinsic;
  exports org.smallmind.persistence.cache.praxis.intrinsic;
  exports org.smallmind.persistence.database;
  exports org.smallmind.persistence.database.mysql;
  exports org.smallmind.persistence.orm;
  exports org.smallmind.persistence.orm.aop;
  exports org.smallmind.persistence.orm.hibernate;
  exports org.smallmind.persistence.orm.jpa;
  exports org.smallmind.persistence.orm.querydsl.jpa;
  exports org.smallmind.persistence.orm.reflect;
  exports org.smallmind.persistence.orm.spring;
  exports org.smallmind.persistence.orm.spring.jpa;
  exports org.smallmind.persistence.orm.spring.throng;
  exports org.smallmind.persistence.orm.throng;
  exports org.smallmind.persistence.sql;
  exports org.smallmind.persistence.sql.pool;
  exports org.smallmind.persistence.sql.pool.context;
  exports org.smallmind.persistence.sql.pool.spring;
  exports org.smallmind.persistence.sql.testbench;

  opens org.smallmind.persistence;
  opens org.smallmind.persistence.cache.memcached.spring to spring.core, spring.beans;
  opens org.smallmind.persistence.orm to spring.core, spring.beans;
  opens org.smallmind.persistence.orm.jpa to org.hibernate.orm.core;
  opens org.smallmind.persistence.orm.spring to spring.core, spring.beans;
  opens org.smallmind.persistence.orm.spring.jpa to spring.core, spring.beans;
  opens org.smallmind.persistence.orm.spring.throng to spring.core, spring.beans;
  opens org.smallmind.persistence.orm.throng to org.smallmind.mongodb.throng;
  opens org.smallmind.persistence.sql.pool.spring to spring.core, spring.beans;
}
