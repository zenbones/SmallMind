package org.smallmind.persistence.model.type.converter;

import org.smallmind.persistence.model.type.PrimitiveType;

public class DoubleStringConverter implements StringConverter<Double> {

   public PrimitiveType getPrimitiveType () {

      return PrimitiveType.DOUBLE;
   }

   public Double convert (String value) {

      return Double.parseDouble(value);
   }
}
