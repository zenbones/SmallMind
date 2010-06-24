package org.smallmind.scribe.pen;

import java.io.File;

public class FileSizeRollover extends Rollover {

   private FileSizeQuantifier fileSizeQuantifier;
   private long maxSize;

   public FileSizeRollover () {

      this(10, FileSizeQuantifier.MEGABYTES);
   }

   public FileSizeRollover (long maxSize, FileSizeQuantifier fileSizeQuantifier) {

      super();

      this.maxSize = maxSize;
      this.fileSizeQuantifier = fileSizeQuantifier;
   }

   public FileSizeRollover (long maxSize, FileSizeQuantifier fileSizeQuantifier, char separator, Timestamp timestamp) {

      super(separator, timestamp);

      this.maxSize = maxSize;
      this.fileSizeQuantifier = fileSizeQuantifier;
   }

   public long getMaxSize () {

      return maxSize;
   }

   public void setMaxSize (long maxSize) {

      this.maxSize = maxSize;
   }

   public FileSizeQuantifier getFileSizeQuantifier () {

      return fileSizeQuantifier;
   }

   public void setFileSizeQuantifier (FileSizeQuantifier fileSizeQuantifier) {

      this.fileSizeQuantifier = fileSizeQuantifier;
   }

   public boolean willRollover (File logFile, long bytesToBeWritten) {

      return (logFile.length() + bytesToBeWritten) > (maxSize * fileSizeQuantifier.getMultiplier());
   }
}