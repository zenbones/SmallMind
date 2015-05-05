/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.lang.ArrayUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

public class LongArrayUserType implements UserType {

  protected static final int SQL_TYPE = java.sql.Types.ARRAY;

  @Override
  public Object nullSafeGet (final ResultSet rs, final String[] names, final SessionImplementor sessionImplementor, final Object owner)
    throws HibernateException, SQLException {

    Array array = rs.getArray(names[0]);
    Long[] javaArray = (Long[])array.getArray();
    return ArrayUtils.toPrimitive(javaArray);
  }

  @Override
  public void nullSafeSet (final PreparedStatement statement, final Object object, final int i, final SessionImplementor sessionImplementor)
    throws HibernateException, SQLException {

    Connection connection = statement.getConnection();

    long[] castObject = (long[])object;
    Long[] longs = ArrayUtils.toObject(castObject);
    Array array = connection.createArrayOf("long", longs);

    statement.setArray(i, array);
  }

  @Override
  public Object assemble (final Serializable cached, final Object owner)
    throws HibernateException {

    return cached;
  }

  @Override
  public Object deepCopy (final Object o)
    throws HibernateException {

    return o == null ? null : ((int[])o).clone();
  }

  @Override
  public Serializable disassemble (final Object o)
    throws HibernateException {

    return (Serializable)o;
  }

  @Override
  public boolean equals (final Object x, final Object y)
    throws HibernateException {

    return x == null ? y == null : x.equals(y);
  }

  @Override
  public int hashCode (final Object o)
    throws HibernateException {

    return o == null ? 0 : o.hashCode();
  }

  @Override
  public boolean isMutable () {

    return false;
  }

  @Override
  public Object replace (final Object original, final Object target, final Object owner)
    throws HibernateException {

    return original;
  }

  @Override
  public Class<long[]> returnedClass () {

    return long[].class;
  }

  @Override
  public int[] sqlTypes () {

    return new int[]{SQL_TYPE};
  }
}