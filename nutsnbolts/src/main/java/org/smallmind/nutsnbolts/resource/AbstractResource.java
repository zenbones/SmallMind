package org.smallmind.nutsnbolts.resource;

public abstract class AbstractResource implements Resource {

   private String path;

   public AbstractResource (String path) {

      this.path = path;
   }

   public String getIdentifier () {

      return getScheme() + ":" + getPath();
   }

   public String getPath () {

      return path;
   }

   public void setPath (String path) {

      this.path = path;
   }

   public String toString () {

      return getIdentifier();
   }
}