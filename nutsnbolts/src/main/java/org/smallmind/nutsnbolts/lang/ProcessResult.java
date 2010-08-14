package org.smallmind.nutsnbolts.lang;

public class ProcessResult {

   private String output;
   private int exitStatus;

   public ProcessResult(int exitStatus, String output) {

      this.exitStatus = exitStatus;
      this.output = output;
   }

   public int getExitStatus() {


      return exitStatus;
   }

   public String getOutput() {

      return output;
   }
}
