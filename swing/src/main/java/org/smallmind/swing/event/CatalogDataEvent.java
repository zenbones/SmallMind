package org.smallmind.swing.event;

import java.util.EventObject;

public class CatalogDataEvent extends EventObject {

   private int startIndex;
   private int endIndex;

   public CatalogDataEvent (Object source, int startIndex, int endIndex) {

      super(source);

      this.startIndex = startIndex;
      this.endIndex = endIndex;
   }

   public int getStartIndex () {

      return startIndex;
   }

   public int getEndIndex () {

      return endIndex;
   }

}
