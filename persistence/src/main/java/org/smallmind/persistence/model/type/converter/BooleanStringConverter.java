package org.smallmind.persistence.model.type.converter;

import org.smallmind.persistence.model.type.PrimitiveType;

public class BooleanStringConverter implements StringConverter<Boolean> {

   public PrimitiveType getPrimitiveType () {

      return PrimitiveType.BOOLEAN;
   }

   public Boolean convert (String value) {

      return Boolean.parseBoolean(value);
   }
}
