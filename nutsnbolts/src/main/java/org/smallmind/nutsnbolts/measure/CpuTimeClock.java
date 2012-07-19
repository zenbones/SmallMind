package org.smallmind.nutsnbolts.measure;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class CpuTimeClock extends Clock {

  private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

  @Override
  public long getTick () {

    return THREAD_MX_BEAN.getCurrentThreadCpuTime();
  }
}

