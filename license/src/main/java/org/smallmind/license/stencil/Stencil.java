package org.smallmind.license.stencil;

public class Stencil {

   private String id;
   private String skipLines;
   private String firstLine;
   private String lastLine;
   private String beforeEachLine;
   private boolean allowBlankLines = false;

   public String getId () {

      return id;
   }

   public void setId (String id) {

      this.id = id;
   }

   public String getSkipLines () {

      return skipLines;
   }

   public void setSkipLines (String skipLines) {

      this.skipLines = skipLines;
   }

   public String getFirstLine () {

      return firstLine;
   }

   public void setFirstLine (String firstLine) {

      this.firstLine = firstLine;
   }

   public String getLastLine () {

      return lastLine;
   }

   public void setLastLine (String lastLine) {

      this.lastLine = lastLine;
   }

   public String getBeforeEachLine () {

      return beforeEachLine;
   }

   public void setBeforeEachLine (String beforeEachLine) {

      this.beforeEachLine = beforeEachLine;
   }

   public boolean isAllowBlankLines () {

      return allowBlankLines;
   }

   public void setAllowBlankLines (boolean allowBlankLines) {

      this.allowBlankLines = allowBlankLines;
   }
}