package org.smallmind.persistence.model.type.converter;

import org.smallmind.persistence.model.type.PrimitiveType;

public class ByteStringConverter implements StringConverter<Byte> {

   public PrimitiveType getPrimitiveType () {

      return PrimitiveType.BYTE;
   }

   public Byte convert (String value) {

      return Byte.parseByte(value);
   }
}
