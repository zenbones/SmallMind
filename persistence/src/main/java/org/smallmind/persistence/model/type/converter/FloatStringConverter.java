package org.smallmind.persistence.model.type.converter;

import org.smallmind.persistence.model.type.PrimitiveType;

public class FloatStringConverter implements StringConverter<Float> {

   public PrimitiveType getPrimitiveType () {

      return PrimitiveType.FLOAT;
   }

   public Float convert (String value) {

      return Float.parseFloat(value);
   }
}
