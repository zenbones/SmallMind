package org.smallmind.nutsnbolts.util;

public class Switch {

   private boolean state;

   public Switch () {

      this(false);
   }

   public Switch (boolean state) {

      this.state = state;
   }

   public void flip () {

      state = !state;
   }

   public void setState (boolean state) {

      this.state = state;
   }

   public boolean isOn () {

      return state;
   }

}
