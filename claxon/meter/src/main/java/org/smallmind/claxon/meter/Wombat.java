package org.smallmind.claxon.meter;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.smallmind.claxon.meter.aggregate.Clocked;
import org.smallmind.nutsnbolts.time.Stint;

public class Wombat {

  public static void main (String... args)
    throws Exception {

    Clocked clocked = new Clocked("foo", TimeUnit.SECONDS, new Stint(100, TimeUnit.MILLISECONDS));

    for (int i = 0; i < 10; i++) {
      new Thread(new Worker(clocked)).start();
    }
    new Thread(new Reader(clocked)).start();

    Thread.sleep(3000000);
  }

  private static class Reader implements Runnable {

    private Clocked clocked;

    public Reader (Clocked clocked) {

      this.clocked = clocked;
    }

    @Override
    public void run () {

      while (true) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {

        }
        System.out.println(clocked.getVelocity());
      }
    }
  }

  private static class Worker implements Runnable {

    private Clocked clocked;

    public Worker (Clocked clocked) {

      this.clocked = clocked;
    }

    @Override
    public void run () {

      while (true) {

        long value = ThreadLocalRandom.current().nextLong(100, 300);

        clocked.update(value);

        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {

        }
      }
    }
  }
}
