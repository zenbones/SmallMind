package org.smallmind.nagios;

public enum ReturnCode {

   OK(0), WARN(1), CRITICAL(2), UNKNOWN(3);

   private int code;

   private ReturnCode (int code) {

      this.code = code;
   }

   public int getCode () {

      return code;
   }
}


