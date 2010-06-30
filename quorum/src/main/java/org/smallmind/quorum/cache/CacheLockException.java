package org.smallmind.quorum.cache;

public class CacheLockException extends CacheException {

   public CacheLockException () {

      super();
   }

   public CacheLockException (String message, Object... args) {

      super(message, args);
   }

   public CacheLockException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public CacheLockException (Throwable throwable) {

      super(throwable);
   }
}
