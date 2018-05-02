/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.web.jersey.persistence.hibernate;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.TextType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.smallmind.nutsnbolts.json.VersionedJson;
import org.smallmind.nutsnbolts.version.Version;
import org.smallmind.nutsnbolts.version.VersionFactory;
import org.smallmind.web.jersey.util.JsonCodec;

public class VersionedJsonUserType<V extends Version<V>> implements UserType, ParameterizedType {

  private VersionFactory<V> versionFactory;

  public void setParameterValues (Properties parameters) {

    String versionFactoryClassName = parameters.getProperty("versionFactoryClassName");

    try {

      Class<? extends VersionFactory<V>> versionFactoryClass;

      versionFactoryClass = (Class<? extends VersionFactory<V>>)Class.forName(versionFactoryClassName);
      versionFactory = versionFactoryClass.newInstance();
    } catch (Exception exception) {
      throw new HibernateException(exception);
    }
  }

  @Override
  public boolean isMutable () {

    return false;
  }

  @Override
  public Class returnedClass () {

    return VersionedJson.class;
  }

  @Override
  public int[] sqlTypes () {

    return new int[] {Types.VARCHAR, TextType.INSTANCE.sqlType()};
  }

  @Override
  public Object assemble (Serializable cached, Object owner) {

    return cached;
  }

  @Override
  public Serializable disassemble (Object value) {

    return (Serializable)value;
  }

  @Override
  public Object deepCopy (Object value) {

    return value;
  }

  @Override
  public Object replace (Object original, Object target, Object owner) {

    return original;
  }

  @Override
  public boolean equals (Object x, Object y) {

    return (x == y) || ((x != null) && x.equals(y));
  }

  @Override
  public int hashCode (Object x) {

    return (x == null) ? 0 : x.hashCode();
  }

  @Override
  public Object nullSafeGet (ResultSet resultSet, String[] names, SessionImplementor sessionImplementor, Object o)
    throws HibernateException, SQLException {
//  @Override
//  public Object nullSafeGet (ResultSet resultSet, String[] names, SharedSessionContractImplementor sharedSessionContractImplementor, Object o)
//    throws HibernateException, SQLException {

    String versionAsString;

    if ((versionAsString = resultSet.getString(names[0])) != null) {

      V version = versionFactory.fromString(versionAsString);
      String jsonAsString;

      if ((jsonAsString = resultSet.getString(names[1])) != null) {

        try {
          return JsonCodec.read(jsonAsString, version.getVersionedClass());
        } catch (IOException ioException) {
          throw new HibernateException(ioException);
        }
      }
    }

    return null;
  }

  @Override
  public void nullSafeSet (PreparedStatement preparedStatement, Object value, int index, SessionImplementor sessionImplementor)
    throws HibernateException, SQLException {
//  @Override
//  public void nullSafeSet (PreparedStatement preparedStatement, Object value, int index, SharedSessionContractImplementor sharedSessionContractImplementor)
//    throws HibernateException, SQLException {

    if (value == null) {
      preparedStatement.setNull(index, Types.VARCHAR);
      preparedStatement.setNull(index + 1, TextType.INSTANCE.sqlType());
    } else {

      preparedStatement.setString(index, ((VersionedJson<?>)value).getVersion().toString());
      try {
        preparedStatement.setString(index + 1, JsonCodec.writeAsString(value));
      } catch (JsonProcessingException jsonProcessingException) {
        throw new HibernateException(jsonProcessingException);
      }
    }
  }
}