package org.smallmind.claxon.meter;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.HdrHistogram.Histogram;
import org.smallmind.claxon.meter.aggregate.Stratified;

public class Wombat {

  public static void main (String... args)
    throws Exception {

    Stratified stratified = new Stratified("foo", TimeUnit.SECONDS);

    for (int i = 0; i < 10; i++) {
      new Thread(new Worker(stratified)).start();
    }
    new Thread(new Reader(stratified)).start();

    Thread.sleep(3000000);
  }

  private static class Reader implements Runnable {

    private Stratified stratified;
    private Histogram histogram;

    public Reader (Stratified stratified) {

      this.stratified = stratified;
    }

    @Override
    public void run () {

      while (true) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {

        }

        histogram = stratified.get();

        System.out.println(histogram.getMean() + ":" + histogram.getMaxValue() + ":" + histogram.getTotalCount());
      }
    }
  }

  private static class Worker implements Runnable {

    private Stratified stratified;

    public Worker (Stratified stratified) {

      this.stratified = stratified;
    }

    @Override
    public void run () {

      while (true) {

        long value = ThreadLocalRandom.current().nextLong(100, 300);

        stratified.update(value);

        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {

        }
      }
    }
  }
}
