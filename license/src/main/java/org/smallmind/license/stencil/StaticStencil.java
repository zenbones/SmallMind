package org.smallmind.license.stencil;

public class StaticStencil extends Stencil {

   @Override
   public final String getId () {

      return this.getClass().getName();
   }

   @Override
   public final void setId (String id) {

      throw new UnsupportedOperationException();
   }

   @Override
   public final void setSkipLines (String skipLines) {

      throw new UnsupportedOperationException();
   }

   @Override
   public final void setFirstLine (String firstLine) {

      throw new UnsupportedOperationException();
   }

   @Override
   public final void setLastLine (String lastLine) {

      throw new UnsupportedOperationException();
   }

   @Override
   public final void setBeforeEachLine (String beforeEachLine) {

      throw new UnsupportedOperationException();
   }

   @Override
   public final void setAllowBlankLines (boolean allowBlankLines) {

      throw new UnsupportedOperationException();
   }
}
