package org.smallmind.nutsnbolts.measure;

public enum Clocks {

  NANO(new NanoClock()), CPU_TIME(new CpuTimeClock());

  private Clock clock;

  private Clocks (Clock clock) {

    this.clock = clock;
  }

  public Clock getClock () {

    return clock;
  }
}
