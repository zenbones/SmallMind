package org.smallmind.persistence.statistics;

public enum StatSource {

   ORM("Orm"), TERRACOTTA("Terracotta");

   private String display;

   private StatSource (String display) {

      this.display = display;
   }

   public String getDisplay () {

      return display;
   }
}
