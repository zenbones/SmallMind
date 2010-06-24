package org.smallmind.persistence.orm.hibernate;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

public class BlobUserType implements UserType {

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

      return x.hashCode();
   }

   public Object replace (Object original, Object target, Object owner)
      throws HibernateException {

      return original;
   }

   public boolean equals (Object x, Object y) {

      return (x == y) || (x != null && y != null && java.util.Arrays.equals((byte[])x, (byte[])y));
   }

   public Object nullSafeGet (ResultSet rs, String[] names, Object owner)
      throws HibernateException, SQLException {

      Blob blob = rs.getBlob(names[0]);

      return blob.getBytes(1, (int)blob.length());
   }

   public void nullSafeSet (PreparedStatement st, Object value, int index)
      throws HibernateException, SQLException {

      st.setBlob(index, Hibernate.createBlob((byte[])value));
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
