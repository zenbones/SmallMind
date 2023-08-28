package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.scribe.pen.LoggerManager;

public class ExpiredChannelSifter<V extends Value<V>> implements Runnable {

  private final CountDownLatch finishLatch = new CountDownLatch(1);
  private final CountDownLatch exitLatch = new CountDownLatch(1);
  private final ChannelTree<V> channelTree;
  private final Consumer<Channel<V>> channelCallback;
  private final long expiredChannelCycleMinutes;

  public ExpiredChannelSifter (long expiredChannelCycleMinutes, ChannelTree<V> channelTree, Consumer<Channel<V>> channelCallback) {

    this.expiredChannelCycleMinutes = expiredChannelCycleMinutes;
    this.channelTree = channelTree;
    this.channelCallback = channelCallback;
  }

  public void stop ()
    throws InterruptedException {

    finishLatch.countDown();
    exitLatch.await();
  }

  @Override
  public void run () {

    try {
      while (!finishLatch.await(expiredChannelCycleMinutes, TimeUnit.MINUTES)) {
        channelTree.walk(new ExpirationOperation<V>(System.currentTimeMillis(), channelCallback));
        channelTree.clean();
      }
    } catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(OumuamuaServer.class).error(interruptedException);
    } finally {
      exitLatch.countDown();
    }
  }
}
