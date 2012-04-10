/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

public class SerializableBlobUserType implements UserType, ParameterizedType {

  private String dataSource;
  private boolean compressed;
  private boolean immutable;

  public void setParameterValues (Properties parameters) {

    dataSource = parameters.getProperty("dataSource");
    compressed = Boolean.parseBoolean(parameters.getProperty("compressed", "false"));
    immutable = Boolean.parseBoolean(parameters.getProperty("immutable", "false"));
  }

  public int[] sqlTypes () {

    return new int[] {Types.BLOB};
  }

  public Class returnedClass () {

    return Serializable.class;
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

    return x.hashCode();
  }

  public Object replace (Object original, Object target, Object owner)
    throws HibernateException {

    return original;
  }

  public boolean equals (Object x, Object y)
    throws HibernateException {

    return x == y || (x != null && y != null && x.equals(y));
  }

  @Override
  public Object nullSafeGet (ResultSet rs, String[] names, SessionImplementor session, Object owner)
    throws HibernateException, SQLException {

    Blob blob = rs.getBlob(names[0]);

    return fromByteArray(blob.getBytes(1, (int)blob.length()));
  }

  @Override
  public void nullSafeSet (PreparedStatement st, Object value, int index, SessionImplementor session)
    throws HibernateException, SQLException {

    st.setBlob(index, Hibernate.getLobCreator(session).createBlob(toByteArray(value)));
  }

  public Object deepCopy (Object value)
    throws HibernateException {

    if (immutable) {

      return value;
    }
    if (value instanceof Cloneable) {
      try {

        return value.getClass().getMethod("clone").invoke(value);
      }
      catch (Exception exception) {
        throw new HibernateException(exception);
      }
    }
    else {
      try {

        Constructor<?> copyConstructor = value.getClass().getConstructor(value.getClass());

        return copyConstructor.newInstance(value);
      }
      catch (Exception exception) {

        return fromByteArray(toByteArray(value));
      }
    }
  }

  public boolean isMutable () {

    return !immutable;
  }

  private byte[] toByteArray (Object value) {

    if (value == null) return null;

    ByteArrayOutputStream baos = new ByteArrayOutputStream((value instanceof SizeAwareSerializable) ? ((SizeAwareSerializable)value).approximateSize() / (compressed ? 4 : 1) : 1024);
    ObjectOutputStream oos;

    try {
      (oos = compressed ? new ObjectOutputStream(new GZIPOutputStream(baos)) : new ObjectOutputStream(baos)).writeObject(value);
      oos.close();

      return baos.toByteArray();
    }
    catch (IOException ioException) {
      throw new HibernateException(ioException);
    }
  }

  private Object fromByteArray (byte[] array) {

    try {

      ByteArrayInputStream bais = new ByteArrayInputStream(array);
      ObjectInputStream ois = compressed ? new ObjectInputStream(new GZIPInputStream(bais)) : new ObjectInputStream(bais);
      Object value;

      value = ois.readObject();
      ois.close();

      return value;
    }
    catch (Exception exception) {
      throw new HibernateException(exception);
    }
  }
}
