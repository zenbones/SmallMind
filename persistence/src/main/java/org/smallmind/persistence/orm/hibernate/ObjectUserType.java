/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

public class ObjectUserType implements UserType, ParameterizedType {

   private Class embeddedClass;

   public void setParameterValues (Properties parameters) {

      String objectClassName = parameters.getProperty("embeddedClassName");

      try {
         embeddedClass = Class.forName(objectClassName);

         if (!Serializable.class.isAssignableFrom(embeddedClass)) {
            throw new HibernateException("Embedded class(" + embeddedClass + ") must be Serializable");
         }
      }
      catch (ClassNotFoundException cnfe) {
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

   public Object nullSafeGet (ResultSet rs, String[] names, Object owner)
      throws HibernateException, SQLException {

      byte[] bytes = rs.getBytes(names[0]);

      try {
         return rs.wasNull() ? null : new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
      }
      catch (Exception exception) {
         throw new HibernateException(exception);
      }
   }

   public void nullSafeSet (PreparedStatement st, Object value, int index)
      throws HibernateException, SQLException {

      if (value == null) {
         st.setNull(index, Types.VARBINARY);
      }
      else {

         ByteArrayOutputStream byteStream;

         try {
            new ObjectOutputStream(byteStream = new ByteArrayOutputStream()).writeObject(value);
         }
         catch (IOException ioException) {
            throw new HibernateException(ioException);
         }

         st.setBytes(index, byteStream.toByteArray());
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

      return new int[] {Types.VARBINARY};
   }
}