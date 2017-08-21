package org.smallmind.nutsnbolts.retry;

public interface RetryCall {

  void execute ()
    throws Throwable;
}
