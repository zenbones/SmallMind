package org.smallmind.phalanx.wire.jms.hornetq;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;
import org.hornetq.api.core.client.loadbalance.ConnectionLoadBalancingPolicy;

public class RoundRobinConnectionLoadBalancingPolicy implements ConnectionLoadBalancingPolicy, Serializable {

  private boolean initialized = false;
  private int pos;

  public synchronized int select (final int max) {

    if (!initialized) {
      initialized = true;

      return pos = ThreadLocalRandom.current().nextInt(max);
    } else {
      if (++pos >= max) {
        pos = 0;
      }

      return pos;
    }
  }
}