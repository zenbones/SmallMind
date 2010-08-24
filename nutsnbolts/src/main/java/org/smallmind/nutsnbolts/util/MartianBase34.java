package org.smallmind.nutsnbolts.util;

public class MartianBase34 {

   public static enum Group {

      FIRST(648913), SECOND(247123), THIRD(7294612383675L);

      private long mixConstant;

      private Group (long mixConstant) {

         this.mixConstant = mixConstant;
      }

      public long getMixConstant () {

         return mixConstant;
      }
   }

   public static final String NUMEROLOGY = "QYEN0MT2PLCW1UF9X8DBZK3A6SR4HVG7J5";

   private static long getMaxMartian (int digits) {

      return (long)Math.pow(34, digits);
   }

   public static String base10To34 (long base10, Group group) {

      if (base10 < 0) {
         throw new IllegalArgumentException("Base 10 number(" + base10 + ") must be >=0");
      }

      for (int digits = 1; digits <= 13; digits++) {
         if (Math.pow(34, digits) >= base10) {
            return base10To34(base10, digits, group);
         }
      }

      throw new IllegalArgumentException("Base 10 number(" + base10 + ") must be no greater than a long value - what language are we speaking here?");
   }

   public static String base10To34 (long base10, int digits, Group group) {

      StringBuilder martianBuilder = new StringBuilder();
      long maxMartian = getMaxMartian(digits);
      long currentResult;

      if ((base10 < 0) || (base10 > maxMartian)) {
         throw new IllegalArgumentException("Base 10 number(" + base10 + ") must be >=0 and <" + maxMartian);
      }

      currentResult = group.getMixConstant() + base10;
      if (currentResult >= maxMartian) {
         currentResult -= maxMartian;
      }

      while (currentResult != 0) {
         martianBuilder.insert(0, NUMEROLOGY.charAt((int)(currentResult % 34)));
         currentResult /= 34;
      }

      while (martianBuilder.length() < digits) {
         martianBuilder.insert(0, NUMEROLOGY.charAt(0));
      }

      return martianBuilder.toString();
   }

   public static long base34To10 (String base34, Group group) {

      long base10 = 0;
      long multiplier = 1;
      int martianNumber;

      for (int index = base34.length() - 1; index >= 0; index--) {
         if ((martianNumber = NUMEROLOGY.indexOf(base34.charAt(index))) < 0) {
            throw new IllegalArgumentException("Not a Martian base 34 number(" + base34 + ")");
         }

         base10 += martianNumber * multiplier;
         multiplier *= 34;
      }

      base10 -= group.getMixConstant();
      if (base10 < 0) {
         base10 += getMaxMartian(base34.length());
      }

      return (base10 >= 0) ? base10 : (Long.MAX_VALUE + base10 + 1) * -1;
   }
}

