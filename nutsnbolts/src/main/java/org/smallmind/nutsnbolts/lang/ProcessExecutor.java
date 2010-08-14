package org.smallmind.nutsnbolts.lang;

import java.io.BufferedInputStream;
import java.io.File;

public class ProcessExecutor {

   public static enum StreamType {

      INPUT_STREAM, ERROR_STREAM
   }

   public static ProcessResult execute(String command, StreamType expectedStream, boolean redirectErrorStream, File directory, boolean waitForProcess)
         throws ExternalProcessException {

      ProcessBuilder processBuilder;
      Process process;
      BufferedInputStream processStream;
      StringBuilder resultBuilder;
      int exitStatus;
      int singleChar;

      try {
         processBuilder = new ProcessBuilder(command.split(" "));
         processBuilder.redirectErrorStream(redirectErrorStream);

         if (directory != null) {
            processBuilder.directory(directory);
         }

         process = processBuilder.start();

         if (waitForProcess) {
            exitStatus = process.waitFor();
         } else {
            exitStatus = 0;
         }

         switch (expectedStream) {
            case INPUT_STREAM:
               if (redirectErrorStream || (exitStatus == 0)) {
                  processStream = new BufferedInputStream(process.getInputStream());
                  break;
               }
            case ERROR_STREAM:
               processStream = new BufferedInputStream(process.getErrorStream());
               break;
            default:
               throw new UnknownSwitchCaseException(expectedStream.name());
         }

         resultBuilder = new StringBuilder();
         while ((singleChar = processStream.read()) != -1) {
            resultBuilder.append((char) singleChar);
         }
         processStream.close();
      }
      catch (Exception e) {
         throw new ExternalProcessException(e);
      }

      if ((exitStatus != 0) || ((expectedStream.equals(StreamType.ERROR_STREAM)) && (resultBuilder.length() > 0))) {
         throw new ExternalProcessException("Exception during process execution...\n\t%s", resultBuilder.toString());
      }

      return new ProcessResult(exitStatus, resultBuilder.toString());
   }
}