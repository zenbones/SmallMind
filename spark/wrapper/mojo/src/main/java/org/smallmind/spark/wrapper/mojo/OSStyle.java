package org.smallmind.spark.wrapper.mojo;

public enum OSStyle {

   WINDOWS("wrapper.dll"), UNIX("libwrapper.so");

   private String library;

   private OSStyle (String library) {

      this.library = library;
   }

   public String getLibrary () {

      return library;
   }
}
