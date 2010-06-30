package org.smallmind.persistence.model.type;

import java.util.Date;

public class TypeUtility {

   private static final Class[] PRIMITIVE_EQUIVALENTS = new Class[] {Date.class, String.class, Long.class, Boolean.class, Integer.class, Double.class, Float.class, Character.class, Short.class, Byte.class};
/*
  public static MetaType getMetaType (Type type) {

    System.out.println("..........3:" + field.getName() + ":" + field.getGenericType());
    if (type instanceof Class) {
      Class currentClass = (Class)field.getGenericType();

      if (currentClass.isPrimitive()) {
        if (currentClass.equals(long.class)) {
          return Long.class;
        }
        if (currentClass.equals(char.class)) {
          return Character.class;
        }
        if (currentClass.equals(int.class)) {
          return Integer.class;
        }
        if (currentClass.equals(byte.class)) {
          return Byte.class;
        }
        if (currentClass.equals(short.class)) {
          return Short.class;
        }
        if (currentClass.equals(float.class)) {
          return Float.class;
        }
        if (currentClass.equals(double.class)) {
          return Double.class;
        }
        if (currentClass.equals(boolean.class)) {
          return Boolean.class;
        }
      }
      else if (currentClass.isEnum()) {
        return Enum.class;
      }

      for (Class primitiveClass : PRIMITIVE_EQUIVALENTS) {
        if (currentClass.equals(primitiveClass)) {

          return primitiveClass;
        }
      }
    }

    if (field.getGenericType() instanceof TypeVariable) {
      for (Type ft : ((TypeVariable)field.getGenericType()).getBounds()) {
        System.out.println("!!!!!!!:" + ft.getClass() + ":" + ft);
      }
    }

    return null;
  }
  */
}
