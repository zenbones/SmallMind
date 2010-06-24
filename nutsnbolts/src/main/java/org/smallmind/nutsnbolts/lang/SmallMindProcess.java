package org.smallmind.nutsnbolts.lang;

import java.io.BufferedInputStream;

public class SmallMindProcess {

   public static enum StreamType {

      INPUT_STREAM, ERROR_STREAM
   }

   public static String execute (String command, StreamType expectedStream, boolean waitForProcess)
      throws ExternalProcessException {

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
                  processStream = new BufferedInputStream(process.getInputStream());
                  break;
               }
            case ERROR_STREAM:
               processStream = new BufferedInputStream(process.getErrorStream());
               break;
            default:
               throw new UnknownSwitchCaseException(expectedStream.name());
         }

         processBuilder = new StringBuilder();
         while ((singleChar = processStream.read()) != -1) {
            processBuilder.append((char)singleChar);
         }
         processStream.close();
      }
      catch (Exception e) {
         throw new ExternalProcessException(e);
      }

      if ((exitStatus != 0) || ((expectedStream.equals(StreamType.ERROR_STREAM)) && (processBuilder.length() > 0))) {
         throw new ExternalProcessException("Exception during process execution...\n\t%s", processBuilder.toString());
      }

      return processBuilder.toString();
   }
}