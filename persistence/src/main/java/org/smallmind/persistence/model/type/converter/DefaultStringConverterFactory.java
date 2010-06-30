package org.smallmind.persistence.model.type.converter;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.persistence.model.bean.BeanAccessException;
import org.smallmind.persistence.model.type.PrimitiveType;

public class DefaultStringConverterFactory implements StringConverterFactory {

   private static final StringConverterFactory INSTANCE = new DefaultStringConverterFactory();
   private static final Class[] KNOWN_CONVERSIONS = new Class[] {Long.class, Character.class, Integer.class, Byte.class, Short.class, Float.class, Double.class, Boolean.class, String.class, Date.class};

   private final ConcurrentHashMap<Class, StringConverter<?>> converterMap = new ConcurrentHashMap<Class, StringConverter<?>>();

   public static StringConverterFactory getInstance () {

      return INSTANCE;
   }

   public DefaultStringConverterFactory () {

      this(new DateStringConverter());
   }

   public DefaultStringConverterFactory (StringConverter dateStringConverter) {

      if (!dateStringConverter.getPrimitiveType().equals(PrimitiveType.DATE)) {
         throw new IllegalArgumentException("Optional StringConverter for 'Date' must declare the proper PrimitiveType(" + dateStringConverter.getPrimitiveType().name() + ")");
      }

      converterMap.put(Long.class, new LongStringConverter());
      converterMap.put(Character.class, new CharacterStringConverter());
      converterMap.put(Integer.class, new IntegerStringConverter());
      converterMap.put(Byte.class, new ByteStringConverter());
      converterMap.put(Short.class, new ShortStringConverter());
      converterMap.put(Float.class, new FloatStringConverter());
      converterMap.put(Double.class, new DoubleStringConverter());
      converterMap.put(Boolean.class, new BooleanStringConverter());
      converterMap.put(String.class, new StringStringConverter());

      converterMap.put(Date.class, dateStringConverter);
   }

   public StringConverter getStringConverter (Class parameterClass)
      throws BeanAccessException {

      Class convergedClass;

      if ((convergedClass = getConvergedClass(parameterClass)).isEnum()) {

         StringConverter stringConverter;

         if ((stringConverter = converterMap.get(convergedClass)) == null) {
            converterMap.put(convergedClass, stringConverter = new EnumStringConverter((Class<? extends Enum<?>>)convergedClass));
         }

         return stringConverter;
      }

      return converterMap.get(convergedClass);
   }

   private static Class getConvergedClass (Class parameterClass)
      throws BeanAccessException {

      if (parameterClass.isPrimitive()) {
         if (parameterClass.equals(long.class)) {
            return Long.class;
         }
         if (parameterClass.equals(char.class)) {
            return Character.class;
         }
         if (parameterClass.equals(int.class)) {
            return Integer.class;
         }
         if (parameterClass.equals(byte.class)) {
            return Byte.class;
         }
         if (parameterClass.equals(short.class)) {
            return Short.class;
         }
         if (parameterClass.equals(float.class)) {
            return Float.class;
         }
         if (parameterClass.equals(double.class)) {
            return Double.class;
         }
         if (parameterClass.equals(boolean.class)) {
            return Boolean.class;
         }
      }

      if (!(parameterClass.isEnum() || isKnownConversion(parameterClass))) {
         throw new BeanAccessException("Can't convert to a 'setter' value of type(%s)", parameterClass.getName());
      }

      return parameterClass;
   }

   private static boolean isKnownConversion (Class parameterClass) {

      for (Class knownClass : KNOWN_CONVERSIONS) {
         if (knownClass.equals(parameterClass)) {

            return true;
         }
      }

      return false;
   }
}
