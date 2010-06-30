package org.smallmind.persistence.model.type.converter;

import org.smallmind.persistence.model.type.PrimitiveType;

public class CharacterStringConverter implements StringConverter<Character> {

   public PrimitiveType getPrimitiveType () {

      return PrimitiveType.CHARACTER;
   }

   public Character convert (String value) {

      return value.charAt(0);
   }
}
