package org.smallmind.nutsnbolts.retry;

public class RetryWorker implements Runnable {

  private Throwable throwable;
  private RetryCall retryCall;

  public RetryWorker (RetryCall retryCall) {

    this.retryCall = retryCall;
  }

  public void reset () {

    throwable = null;
  }

  public boolean isSuccess () {

    return throwable == null;
  }

  @Override
  public void run () {

    try {
      retryCall.execute();
    } catch (Throwable throwable) {
      this.throwable = throwable;
    }
  }
}

