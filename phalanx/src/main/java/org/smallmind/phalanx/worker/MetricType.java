package org.smallmind.phalanx.worker;

public enum MetricType {

  ACQUIRE_WORKER("Acquire Worker"), WORKER_IDLE("Worker Idle");

  private String display;

  MetricType (String display) {

    this.display = display;
  }

  public String getDisplay () {

    return display;
  }
}