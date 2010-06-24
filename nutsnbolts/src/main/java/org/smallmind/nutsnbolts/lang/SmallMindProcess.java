package org.smallmind.nutsnbolts.lang;

import java.io.BufferedInputStream;

public class SmallMindProcess {

   public static enum StreamType {

      INPUT_STREAM, ERROR_STREAM
   }

   ;

   public static String execute (String command, StreamType expectedStream, boolean waitForProcess)
      throws org.smallmind.nutsnbolts.lang.ExternalProcessException {

      Runtime runtime;
      Process process;
      BufferedInputStream processStream;
      StringBuilder processBuilder;
      int exitStatus;
      int singleChar;

      runtime = Runtime.getRuntime();

      try {
         process = runtime.exec(command);

         if (waitForProcess) {
            exitStatus = process.waitFor();
         }
         else {
            exitStatus = 0;
         }

         switch (expectedStream) {
            case INPUT_STREAM:
               if (exitStatus == 0) {
                  processStream = new BuilderedInputStream(process.getInputStream());
                  break;
               }
            case ERROR_STREAM:
               processStream = new BuilderedInputStream(process.getErrorStream());
               break;
            default:
               throw new org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException(expectedStream.name());
         }

         processBuilder = new StringBuilder();
         while ((singleChar = processStream.read()) != -1) {
            processBuilder.append((char)singleChar);
         }
         processStream.close();
      }
      catch (Exception e) {
         throw new org.smallmind.nutsnbolts.lang.ExternalProcessException(e);
      }

      if ((exitStatus != 0) || ((expectedStream.equals(StreamType.ERROR_STREAM)) && (processBuilder.length() > 0))) {
         throw new org.smallmind.nutsnbolts.lang.ExternalProcessException("Exception during process execution...\n\t%s", processBuilder.toString());
      }

      return processBuilder.toString();
   }

}