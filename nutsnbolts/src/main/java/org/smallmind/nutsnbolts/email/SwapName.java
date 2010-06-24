package org.smallmind.nutsnbolts.email;

public class SwapName {

   private static final char[] VALID_ADJECTIVES = {'*'};

   private String name;
   private String adjectives;

   public SwapName (String unparsedName) {

      int adjectivePos;

      adjectivePos = getAdjectivePos(unparsedName);
      name = unparsedName.substring(0, adjectivePos);
      adjectives = unparsedName.substring(adjectivePos);
   }

   public String getName () {

      return name;
   }

   public boolean isAdjective (char adjective) {

      return (adjectives.indexOf(adjective) >= 0);
   }

   private int getAdjectivePos (String unparsedName) {

      int latestPos;
      int adjectivePos = 0;

      for (char valiAdjective : VALID_ADJECTIVES) {
         if ((latestPos = unparsedName.indexOf(valiAdjective)) > adjectivePos) {
            adjectivePos = latestPos;
         }
      }

      if (adjectivePos == 0) {
         return unparsedName.length();
      }

      return adjectivePos;
   }

}