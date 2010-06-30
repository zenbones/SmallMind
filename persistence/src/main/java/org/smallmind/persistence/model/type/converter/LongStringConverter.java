package org.smallmind.persistence.model.type.converter;

import org.smallmind.persistence.model.type.PrimitiveType;

public class LongStringConverter implements StringConverter<Long> {

   public PrimitiveType getPrimitiveType () {

      return PrimitiveType.LONG;
   }

   public Long convert (String value) {

      return Long.parseLong(value);
   }
}
