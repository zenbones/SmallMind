package org.smallmind.scribe.pen;

public enum FileSizeQuantifier {

   BYTES(1), MEGABYTES(1048576);

   private int multiplier;

   private FileSizeQuantifier (int multiplier) {

      this.multiplier = multiplier;
   }

   public int getMultiplier () {

      return multiplier;
   }
}
