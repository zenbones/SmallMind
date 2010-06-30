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