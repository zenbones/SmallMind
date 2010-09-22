package org.smallmind.license.stencil;

public class JavaDocStencil extends StaticStencil {

   @Override
   public String getSkipLines () {

      return null;
   }

   @Override
   public String getFirstLine () {

      return "/*";
   }

   @Override
   public String getLastLine () {

      return "*/";
   }

   @Override
   public String getBeforeEachLine () {

      return "*";
   }

   @Override
   public boolean isPrefixBlankLines () {

      return false;
   }

   @Override
   public int getBlankLinesBefore () {

      return 0;
   }

   @Override
   public int getBlankLinesAfter () {

      return 0;
   }
}
