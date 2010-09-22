package org.smallmind.license;

import java.io.File;

public class Rule {

   private File template;
   private String fileType;
   private String stencil;
   private String[] includes;
   private String[] excludes;

   public File getTemplate () {

      return template;
   }

   public void setTemplate (File template) {

      this.template = template;
   }

   public String getFileType () {

      return fileType;
   }

   public void setFileType (String fileType) {

      this.fileType = fileType;
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
