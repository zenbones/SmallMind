package org.smallmind.persistence.model.type.converter;

import org.smallmind.persistence.model.type.PrimitiveType;

public class IntegerStringConverter implements StringConverter<Integer> {

   public PrimitiveType getPrimitiveType () {

      return PrimitiveType.INTEGER;
   }

   public Integer convert (String value) {

      return Integer.parseInt(value);
   }
}
