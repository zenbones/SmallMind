package org.smallmind.license;

public class Rule {

   private String[] fileTypes;
   private String[] includes;
   private String[] excludes;
   private String id;
   private String name;
   private String stencilId;
   private String license;

   public String getId () {

      return id;
   }

   public void setId (String id) {

      this.id = id;
   }

   public String getName () {

      return name;
   }

   public void setName (String name) {

      this.name = name;
   }

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

   public String getStencilId () {

      return stencilId;
   }

   public void setStencilId (String stencilId) {

      this.stencilId = stencilId;
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
