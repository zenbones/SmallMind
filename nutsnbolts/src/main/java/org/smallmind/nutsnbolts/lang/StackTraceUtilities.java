package org.smallmind.nutsnbolts.lang;

public class StackTraceUtilities {

   public static String obtainStackTrace (Throwable throwable) {

      StackTraceElement[] prevStackTrace = null;
      StringBuilder traceBuilder;
      StringBuilder lineBuilder;
      int repeatedElements;

      traceBuilder = new StringBuilder();
      lineBuilder = new StringBuilder();
      do {
         lineBuilder.append(prevStackTrace == null ? "Exception in thread " : "Caused by: ");

         lineBuilder.append(throwable.getClass().getCanonicalName());
         lineBuilder.append(": ");
         lineBuilder.append(throwable.getMessage());
         traceBuilder.append(lineBuilder);
         lineBuilder.delete(0, lineBuilder.length());

         for (StackTraceElement singleElement : throwable.getStackTrace()) {
            if (prevStackTrace != null) {
               if ((repeatedElements = findRepeatedStackElements(singleElement, prevStackTrace)) >= 0) {
                  lineBuilder.append("   ... ");
                  lineBuilder.append(repeatedElements);
                  lineBuilder.append(" more");
                  traceBuilder.append(lineBuilder);
                  lineBuilder.delete(0, lineBuilder.length());
                  break;
               }
            }

            lineBuilder.append("   at ");
            lineBuilder.append(singleElement);
            traceBuilder.append(lineBuilder);
            lineBuilder.delete(0, lineBuilder.length());
         }

         prevStackTrace = throwable.getStackTrace();
      } while ((throwable = throwable.getCause()) != null);

      return traceBuilder.toString();
   }

   private static int findRepeatedStackElements (StackTraceElement singleElement, StackTraceElement[] prevStackTrace) {

      for (int count = 0; count < prevStackTrace.length; count++)
         if (singleElement.equals(prevStackTrace[count])) return prevStackTrace.length - count;

      return -1;
   }
}
