package org.smallmind.sleuth.runner.event;

import org.smallmind.nutsnbolts.util.AnsiColor;

public class CancelledSleuthEvent extends SleuthEvent {

  public CancelledSleuthEvent (String className, String methodName) {

    super(className, methodName);
  }

  @Override
  public SleuthEventType getType () {

    return SleuthEventType.CANCELLED;
  }

  @Override
  public AnsiColor getColor () {

    return AnsiColor.BRIGHT_RED;
  }
}
