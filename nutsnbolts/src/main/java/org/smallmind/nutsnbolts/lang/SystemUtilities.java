package org.smallmind.nutsnbolts.lang;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

public class SystemUtilities {

   public static String getSystemProperty (String key) {

      return System.getProperty(key);
   }

   public static Properties cloneSystemProperties () {

      return (Properties)System.getProperties().clone();
   }

   public static String getStackTrace (Throwable throwable) {

      StringWriter errorBuffer;
      PrintWriter errorWriter;
      String errorText;

      errorBuffer = new StringWriter();
      errorWriter = new PrintWriter(errorBuffer);
      throwable.printStackTrace(errorWriter);
      errorText = errorBuffer.toString();
      errorWriter.close();

      return errorText;
   }

}
