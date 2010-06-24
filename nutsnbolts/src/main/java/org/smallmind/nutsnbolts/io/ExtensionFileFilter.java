package org.smallmind.nutsnbolts.io;

import java.io.File;
import java.util.LinkedList;
import java.util.regex.Pattern;
import javax.swing.filechooser.FileFilter;

public final class ExtensionFileFilter extends FileFilter implements java.io.FileFilter {

   private LinkedList<Pattern> regExpList;
   private LinkedList<String> extensionList;
   private String description;

   public ExtensionFileFilter () {

      this(null, "");
   }

   public ExtensionFileFilter (String extension) {

      this(extension, "");
   }

   public ExtensionFileFilter (String extension, String description) {

      this.description = description;

      regExpList = new LinkedList<Pattern>();
      extensionList = new LinkedList<String>();

      if (extension != null) {
         addExtension(extension);
      }
   }

   public void addExtension (String extension) {

      Pattern parsedPattern;

      parsedPattern = Pattern.compile(RegExpTranslator.translate("*." + extension));
      regExpList.add(parsedPattern);
      extensionList.add(extension);
   }

   public String getExtension () {

      return extensionList.getFirst();
   }

   public boolean accept (File f) {

      return accept(f.getName());
   }

   public boolean accept (String filename) {

      for (Pattern pattern : regExpList) {
         if (pattern.matcher(filename).matches()) {

            return true;
         }
      }

      return false;
   }

   public void setDescription (String description) {

      this.description = description;
   }

   public String getDescription () {

      StringBuilder fullDescription = new StringBuilder();
      boolean first = true;

      fullDescription.append(description);
      fullDescription.append(" (");

      for (String extension : extensionList) {
         if (!first) {
            fullDescription.append(" ");
         }

         fullDescription.append("*.");
         fullDescription.append(extension);

         first = false;
      }

      fullDescription.append(")");

      return fullDescription.toString();
   }
}
