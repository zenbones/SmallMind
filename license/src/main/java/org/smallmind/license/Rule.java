package org.smallmind.license;

public class Rule {

   private String[] fileTypes;
   private String[] includes;
   private String[] excludes;
   private String license;
   private String stencil;

   public String getLicense () {

      return license;
   }

   public void setLicense (String license) {

      this.license = license;
   }

   public String[] getFileTypes () {

      return fileTypes;
   }

   public void setFileTypes (String[] fileTypes) {

      this.fileTypes = fileTypes;
   }

   public String getStencil () {

      return stencil;
   }

   public void setStencil (String stencil) {

      this.stencil = stencil;
   }

   public String[] getIncludes () {

      return includes;
   }

   public void setIncludes (String[] includes) {

      this.includes = includes;
   }

   public String[] getExcludes () {

      return excludes;
   }

   public void setExcludes (String[] excludes) {

      this.excludes = excludes;
   }
}
