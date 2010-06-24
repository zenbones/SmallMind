package org.smallmind.nutsnbolts.swing.dialog;

public enum OptionType {

   INFO("Info Message", "Info"), WARNING("Warning Message", "Warning"), QUESTION("Question", "Question"), STOP("Error Message", "Stop"), BUG("Bug", "Bug"), PROGRESS("Progress", "Progress");

   private final String title;
   private final String imageType;

   OptionType (String title, String imageType) {

      this.title = title;
      this.imageType = imageType;
   }

   public String getTitle () {

      return title;
   }

   public String getImageType () {

      return imageType;
   }

}
