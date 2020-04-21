package org.smallmind.memcached;

import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.CompressionMode;
import net.rubyeye.xmemcached.transcoders.Transcoder;

public class FastXMemcachedTranscoder<T> implements Transcoder<T> {

  private static final int MAX_SIZE = 1048576;

  @Override
  public boolean isPrimitiveAsString () {

    return false;
  }

  @Override
  public void setPrimitiveAsString (boolean b) {

  }

  @Override
  public boolean isPackZeros () {

    return false;
  }

  @Override
  public void setPackZeros (boolean b) {

  }

  @Override
  public void setCompressionThreshold (int i) {

  }

  @Override
  public void setCompressionMode (CompressionMode compressionMode) {

  }

  @Override
  public CachedData encode (T t) {

    byte[] buffer = new byte[0];

    return new CachedData(1, buffer, MAX_SIZE, -1L);
  }

  @Override
  public T decode (CachedData cachedData) {

    cachedData.getData();
    return null;
  }
}
