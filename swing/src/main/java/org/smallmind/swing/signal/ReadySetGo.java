package org.smallmind.swing.signal;

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public enum ReadySetGo {

   RED, YELLOW, GREEN;

   public ReadySetGo inc () {

      switch (this) {
         case RED:
            return YELLOW;
         case YELLOW:
            return GREEN;
         case GREEN:
            return GREEN;
         default:
            throw new UnknownSwitchCaseException(this.name());
      }
   }

   public ReadySetGo dec () {

      switch (this) {
         case RED:
            return RED;
         case YELLOW:
            return RED;
         case GREEN:
            return YELLOW;
         default:
            throw new UnknownSwitchCaseException(this.name());
      }
   }

}
