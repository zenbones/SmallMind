/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.ParameterizedType;

public class EnumUserType implements ParameterizedType, EnhancedUserType {

  private Class<Enum> enumClass;

  public void setParameterValues (Properties parameters) {

    String enumClassName = parameters.getProperty("enumClassName");

    try {
      enumClass = (Class<Enum>)Class.forName(enumClassName);
    }
    catch (ClassNotFoundException cnfe) {
      throw new HibernateException("Enum class not found", cnfe);
    }
  }

  public int[] sqlTypes () {

    return new int[] {Types.VARCHAR};
  }

  public Class returnedClass () {

    return enumClass;
  }

  public Object assemble (Serializable cached, Object owner)
    throws HibernateException {

    return cached;
  }

  public Serializable disassemble (Object value)
    throws HibernateException {

    return (Enum)value;
  }

  public int hashCode (Object x)
    throws HibernateException {

    return x.hashCode();
  }

  public Object replace (Object original, Object target, Object owner)
    throws HibernateException {

    return original;
  }

  public boolean equals (Object x, Object y)
    throws HibernateException {

    return x == y;
  }

  public Object nullSafeGet (ResultSet rs, String[] names, Object owner)
    throws HibernateException, SQLException {

    String name = rs.getString(names[0]);

    return rs.wasNull() ? null : Enum.valueOf(enumClass, name);
  }

  public void nullSafeSet (PreparedStatement st, Object value, int index)
    throws HibernateException, SQLException {

    if (value == null) {
      st.setNull(index, Types.VARCHAR);
    }
    else {
      st.setString(index, ((Enum)value).name());
    }
  }

  public Object deepCopy (Object value)
    throws HibernateException {

    return value;
  }

  public boolean isMutable () {

    return false;
  }

  public Object fromXMLString (String xmlValue) {

    return Enum.valueOf(enumClass, xmlValue);
  }

  public String objectToSQLString (Object value) {

    return '\'' + ((Enum)value).name() + '\'';
  }

  public String toXMLString (Object value) {

    return ((Enum)value).name();
  }
}