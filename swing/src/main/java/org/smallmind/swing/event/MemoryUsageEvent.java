package org.smallmind.swing.event;

import java.util.EventObject;

public class MemoryUsageEvent extends EventObject {

   private int maximumUsage;
   private int currentUsage;
   private String displayUsage;

   public MemoryUsageEvent (Object source, int maximumUsage, int currentUsage, String displayUsage) {

      super(source);

      this.maximumUsage = maximumUsage;
      this.currentUsage = currentUsage;
      this.displayUsage = displayUsage;
   }

   public int getCurrentUsage () {

      return currentUsage;
   }

   public String getDisplayUsage () {

      return displayUsage;
   }

   public int getMaximumUsage () {

      return maximumUsage;
   }

}
