package org.smallmind.spark.tanukisoft.integration;

import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

public abstract class AbstractWrapperListener implements WrapperListener {

   private static final int NO_ERROR_CODE = 0;
   private static final int STACK_TRACE_ERROR_CODE = 2;

   public abstract void startup (String[] args)
      throws Exception;

   public abstract void shutdown ()
      throws Exception;

   public void controlEvent (int event) {

      if (!WrapperManager.isControlledByNativeWrapper()) {
         if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT) || (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT) || (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT)) {
            WrapperManager.stop(0);
         }
      }
   }

   public Integer start (String[] args) {

      try {
         startup(args);
      }
      catch (Exception exception) {
         exception.printStackTrace();
         return STACK_TRACE_ERROR_CODE;
      }

      return null;
   }

   public int stop (int event) {

      try {
         shutdown();
      }
      catch (Exception exception) {
         exception.printStackTrace();
         return STACK_TRACE_ERROR_CODE;
      }

      return NO_ERROR_CODE;
   }

   public static void main (String... args)
      throws Exception {

      if (args.length < 1) {
         throw new IllegalArgumentException(String.format("First application parameter must be the class of the %s in use", WrapperListener.class.getSimpleName()));
      }
      else {

         String[] trimmedArgs;

         trimmedArgs = new String[args.length - 1];
         System.arraycopy(args, 1, trimmedArgs, 0, args.length - 1);

         WrapperManager.start((WrapperListener)Class.forName(args[0]).newInstance(), trimmedArgs);
      }
   }
}