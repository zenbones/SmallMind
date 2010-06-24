package org.smallmind.nutsnbolts.lang;

public class ClassGateTicket {

   private ClassGate classGate;
   private long timeStamp;

   public ClassGateTicket (ClassGate classGate, long timeStamp) {

      this.classGate = classGate;
      this.timeStamp = timeStamp;
   }

   public ClassGate getClassGate () {

      return classGate;
   }

   public long getTimeStamp () {

      return timeStamp;
   }

}
