package org.smallmind.nutsnbolts.util;

public class DefaultAlphaNumConverter<T> implements AlphaNumericConverter<T> {

   public String toString (T object) {

      return object.toString();
   }

}
