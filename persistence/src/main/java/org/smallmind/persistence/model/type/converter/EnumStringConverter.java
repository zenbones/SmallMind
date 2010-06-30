package org.smallmind.persistence.model.type.converter;

import org.smallmind.persistence.model.type.PrimitiveType;

public class EnumStringConverter<E extends Enum<E>> implements StringConverter<E> {

   private Class<E> enumClass;

   public EnumStringConverter (Class<E> enumClass) {

      this.enumClass = enumClass;
   }

   public PrimitiveType getPrimitiveType () {

      return PrimitiveType.ENUM;
   }

   public E convert (String value) {

      return Enum.valueOf(enumClass, value);
   }
}
