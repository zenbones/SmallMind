package org.smallmind.license.stencil;

public class JavaDocStencil extends StaticStencil {

   @Override
   public String getSkipLines () {

      return null;
   }

   @Override
   public String getFirstLine () {

      return "/* License Header";
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
   public boolean isAllowBlankLines () {

      return false;
   }
}
