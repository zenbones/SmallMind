package org.smallmind.nutsnbolts.io;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

public class WildcardFileNameFileFilter implements FileFilter {

   private Pattern namePattern;

   public WildcardFileNameFileFilter (String name) {

      namePattern = Pattern.compile(RegExpTranslator.translate(name));
   }

   public boolean accept (File file) {

      return namePattern.matcher(file.getName()).matches();
   }
}
