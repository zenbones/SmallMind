package org.smallmind.persistence.model.type.converter;

import org.smallmind.persistence.model.type.PrimitiveType;

public class StringStringConverter implements StringConverter<String> {

   public PrimitiveType getPrimitiveType () {

      return PrimitiveType.STRING;
   }

   public String convert (String value) {

      return value;
   }
}
