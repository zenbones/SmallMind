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
package org.smallmind.web.json.scaffold.persistence.hibernate;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Properties;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.ParameterizedType;
import org.smallmind.web.json.scaffold.version.Version;
import org.smallmind.web.json.scaffold.version.Versioned;

public class VersionedUserType<V extends Enum<V> & Version<V>> implements CompositeUserType<Versioned<V>>, ParameterizedType {

  private Class<V> versionClass;

  public void setParameterValues (Properties parameters) {

    String versionClassName = parameters.getProperty("versionClassName");

    try {
      versionClass = (Class<V>)Class.forName(versionClassName);
    } catch (Exception exception) {
      throw new HibernateException(exception);
    }
  }

  @Override
  public boolean isMutable () {

    return false;
  }

  @Override
  public Class<?> embeddable () {

    return VersionedMapper.class;
  }

  @Override
  public Class<Versioned<V>> returnedClass () {

    return (Class<Versioned<V>>)(Object)versionClass;
  }

  @Override
  public boolean equals (Versioned<V> x, Versioned<V> y) {

    return x == y;
  }

  @Override
  public int hashCode (Versioned<V> x) {

    return Objects.hashCode(x);
  }

  @Override
  public Versioned<V> deepCopy (Versioned<V> value) {

    return value;
  }

  @Override
  public Serializable disassemble (Versioned<V> value) {

    return value;
  }

  @Override
  public Versioned<V> assemble (Serializable cached, Object owner) {

    return (Versioned<V>)cached;
  }

  @Override
  public Versioned<V> replace (Versioned<V> detached, Versioned<V> managed, Object owner) {

    return detached;
  }

  @Override
  public Object getPropertyValue (Versioned<V> component, int property)
    throws HibernateException {

    try {
      // alphabetical
      return switch (property) {
        case 0 -> component.getVersion().toJson(component);
        case 1 -> component.getVersion().name();
        default -> null;
      };
    } catch (JsonProcessingException jsonProcessingException) {
      throw new HibernateException(jsonProcessingException);
    }
  }

  @Override
  public Versioned<V> instantiate (ValueAccess values, SessionFactoryImplementor sessionFactory)
    throws HibernateException {

    try {
      return Enum.valueOf(versionClass, values.getValue(1, String.class)).fromJson(values.getValue(0, String.class));
    } catch (IOException ioException) {
      throw new HibernateException(ioException);
    }
  }

  public static class VersionedMapper {

    String version;
    String json;
  }
}
