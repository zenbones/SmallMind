package org.smallmind.constellation.ephemeral;

public interface Ephemeral {

   public abstract Ephemeral postRevival ()
      throws Exception;

   public abstract void postInvalidation ()
      throws Exception;

}
