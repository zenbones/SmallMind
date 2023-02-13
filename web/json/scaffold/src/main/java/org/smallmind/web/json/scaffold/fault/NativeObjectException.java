package org.smallmind.web.json.scaffold.fault;

import org.smallmind.nutsnbolts.lang.StackTraceUtility;

public class NativeObjectException extends Exception {

  public NativeObjectException (Throwable throwable) {

    super(StackTraceUtility.obtainStackTraceAsString(throwable));
  }
}
