package org.smallmind.nutsnbolts.lang;

import java.io.InputStream;

public class ClassStreamTicket {

   private InputStream inputStream;
   private long timeStamp;

   public ClassStreamTicket (InputStream inputStream, long timeStamp) {

      this.inputStream = inputStream;
      this.timeStamp = timeStamp;
   }

   public InputStream getInputStream () {

      return inputStream;
   }

   public long getTimeStamp () {

      return timeStamp;
   }

}


