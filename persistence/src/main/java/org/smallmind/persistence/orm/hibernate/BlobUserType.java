/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
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
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Properties;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

public class BlobUserType implements UserType, ParameterizedType {

  private String dataSource;

  public void setParameterValues (Properties parameters) {

    dataSource = parameters.getProperty("dataSource");
  }

  public int[] sqlTypes () {

    return new int[] {Types.BLOB};
  }

  public Class returnedClass () {

    return byte[].class;
  }

  public Object assemble (Serializable cached, Object owner)
    throws HibernateException {

    return cached;
  }

  public Serializable disassemble (Object value)
    throws HibernateException {

    return (Serializable)value;
  }

  public int hashCode (Object x)
    throws HibernateException {

    return Arrays.hashCode((byte[])x);
  }

  public Object replace (Object original, Object target, Object owner)
    throws HibernateException {

    return original;
  }

  public boolean equals (Object x, Object y) {

    return (x == y) || ((x instanceof byte[]) && (y instanceof byte[]) && Arrays.equals((byte[])x, (byte[])y));
  }

  @Override
  public Object nullSafeGet (ResultSet rs, String[] names, SessionImplementor session, Object owner)
    throws HibernateException, SQLException {

    Blob blob = rs.getBlob(names[0]);

    return blob.getBytes(1, (int)blob.length());
  }

  @Override
  public void nullSafeSet (PreparedStatement st, Object value, int index, SessionImplementor session)
    throws HibernateException, SQLException {

    st.setBlob(index, Hibernate.getLobCreator(session).createBlob((byte[])value));
  }

  public Object deepCopy (Object value) {

    if (value == null) return null;

    byte[] bytes = (byte[])value;
    byte[] result = new byte[bytes.length];

    System.arraycopy(bytes, 0, result, 0, bytes.length);

    return result;
  }

  public boolean isMutable () {

    return true;
  }
}
