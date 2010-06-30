package org.smallmind.persistence.model.type.converter;

import org.smallmind.persistence.model.type.PrimitiveType;

public class ShortStringConverter implements StringConverter<Short> {

   public PrimitiveType getPrimitiveType () {

      return PrimitiveType.SHORT;
   }

   public Short convert (String value) {

      return Short.parseShort(value);
   }
}
