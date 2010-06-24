package org.smallmind.scribe.pen;

public interface ErrorHandler {

   public abstract void setBackupAppender (Appender appender);

   public abstract void process (Record record, Exception exception, String errorMessage, Object... args);
}