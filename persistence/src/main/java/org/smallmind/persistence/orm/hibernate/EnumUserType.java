/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.persistence.orm.hibernate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

public class EnumUserType implements UserType, ParameterizedType {

  private Class<Enum> enumClass;

  @Override
  public void setParameterValues (Properties parameters) {

    String enumClassName = parameters.getProperty("enumClassName");

    try {
      enumClass = (Class<Enum>)Class.forName(enumClassName);
    } catch (ClassNotFoundException classNotFoundException) {
      throw new HibernateException("Enum class not found", classNotFoundException);
    }
  }

  @Override
  public boolean isMutable () {

    return false;
  }

  @Override
  public Class returnedClass () {

    return enumClass;
  }

  @Override
  public int[] sqlTypes () {

    return new int[]{Types.VARCHAR};
  }

  @Override
  public Object assemble (Serializable cached, Object owner) {

    return cached;
  }

  @Override
  public Serializable disassemble (Object value) {

    return (Enum)value;
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
  public int hashCode (Object x) {

    return (x == null) ? 0 : x.hashCode();
  }

  public boolean equals (Object x, Object y)
    throws HibernateException {

    return x == y;
  }

  @Override
  public Object nullSafeGet (ResultSet rs, String[] names, SessionImplementor session, Object owner)
    throws HibernateException, SQLException {

    String name = rs.getString(names[0]);

    return rs.wasNull() ? null : Enum.valueOf(enumClass, name);
  }

  @Override
  public void nullSafeSet (PreparedStatement st, Object value, int index, SessionImplementor session)
    throws HibernateException, SQLException {

    if (value == null) {
      st.setNull(index, Types.VARCHAR);
    } else {
      st.setString(index, ((Enum)value).name());
    }
  }
}