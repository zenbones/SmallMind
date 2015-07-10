package org.smallmind.forge.deploy;

public class TextProgressBar {

  private String measure;
  private boolean done = false;
  private double total;
  private int segmentPercent;
  private int numberOfSegments;
  private int previousSegment = -1;

  public TextProgressBar (long total, String measure, int segmentPercent) {

    this.total = total;
    this.measure = measure;
    this.segmentPercent = segmentPercent;

    numberOfSegments = (100 / segmentPercent) + ((100 % segmentPercent == 0) ? 0 : 1);
  }

  public synchronized void close () {

    done = true;
    System.out.println();
  }

  public synchronized void update (long current) {

    if (current > total) {
      throw new IllegalArgumentException("Current values must be <= " + total);
    }

    double currentPercent = (current / total) * 100;
    int currentSegment = (int)(currentPercent / segmentPercent);

    if ((!done) && (currentSegment != previousSegment)) {
      System.out.print("\r[");
      for (int tail = 0; tail < currentSegment; tail++) {
        System.out.print("=");
      }
      if (current < total) {
        System.out.print(">");
      }
      for (int blank = currentSegment; blank < (numberOfSegments - 1); blank++) {
        System.out.print(" ");
      }
      System.out.print("] ");

      System.out.print((int)currentPercent);
      System.out.print("% (");

      System.out.print(current);
      System.out.print(" of ");
      System.out.print((long)total);
      System.out.print(" ");
      System.out.print(measure);

      if (current == total) {
        done = true;
        System.out.println(")");
      } else {
        System.out.print(")");
      }
    }

    previousSegment = currentSegment;
  }
}
