package org.smallmind.nutsnbolts.retry;

public class Retry {

  public static boolean execute (RetryCall retryCall, int retries, long delay, boolean exponential)
    throws InterruptedException {

    RetryWorker retryWorker = new RetryWorker(retryCall);
    int count = 0;

    do {
      retryWorker.reset();

      if (count > 0) {
        Thread.sleep(exponential ? delay * count : delay);
      }

      Thread thread = new Thread(retryWorker);

      thread.start();
      thread.join();
    } while ((!retryWorker.isSuccess()) && (count++ < retries));

    return retryWorker.isSuccess();
  }
}