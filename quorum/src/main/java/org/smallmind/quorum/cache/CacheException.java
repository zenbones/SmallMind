package org.smallmind.quorum.cache;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class CacheException extends FormattedRuntimeException {

  public CacheException () {

    super();
  }

  public CacheException (String message, Object... args) {

    super(message, args);
  }

  public CacheException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }

  public CacheException (Throwable throwable) {

    super(throwable);
  }
}
