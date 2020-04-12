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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class JsonUserType implements UserType, ParameterizedType {

  private Class<?> embeddedClass;

  public void setParameterValues (Properties parameters) {

    String objectClassName = parameters.getProperty("embeddedClassName");

    try {
      embeddedClass = Class.forName(objectClassName);
    } catch (ClassNotFoundException cnfe) {
      throw new HibernateException(cnfe);
    }
  }

  public Object assemble (Serializable cached, Object owner)
    throws HibernateException {

    return cached;
  }

  public Object deepCopy (Object value)
    throws HibernateException {

    return value;
  }

  public Serializable disassemble (Object value)
    throws HibernateException {

    return (Serializable)value;
  }

  public boolean equals (Object x, Object y)
    throws HibernateException {

    return x == y;
  }

  public int hashCode (Object x)
    throws HibernateException {

    return x.hashCode();
  }

  public boolean isMutable () {

    return false;
  }

  @Override
  public Object nullSafeGet (ResultSet resultSet, String[] names, SharedSessionContractImplementor sharedSessionContractImplementor, Object o)
    throws HibernateException, SQLException {

    String string = resultSet.getString(names[0]);

    try {
      return resultSet.wasNull() ? null : JsonCodec.read(string, embeddedClass);
    } catch (IOException ioException) {
      throw new HibernateException(ioException);
    }
  }

  @Override
  public void nullSafeSet (PreparedStatement preparedStatement, Object value, int index, SharedSessionContractImplementor sharedSessionContractImplementor)
    throws HibernateException, SQLException {

    if (value == null) {
      preparedStatement.setNull(index, Types.LONGNVARCHAR);
    } else {
      try {
        preparedStatement.setString(index, JsonCodec.writeAsString(value));
      } catch (JsonProcessingException jsonProcessingException) {
        throw new HibernateException(jsonProcessingException);
      }
    }
  }

  public Object replace (Object original, Object target, Object owner)
    throws HibernateException {

    return original;
  }

  public Class returnedClass () {

    return embeddedClass;
  }

  public int[] sqlTypes () {

    return new int[] {Types.LONGVARCHAR};
  }
}