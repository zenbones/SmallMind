/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.memcached;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;

public class XMemcachedMemcachedClient implements ProxyMemcachedClient {

  private MemcachedClient memcachedClient;

  public XMemcachedMemcachedClient (MemcachedClient memcachedClient) {

    this.memcachedClient = memcachedClient;
  }

  @Override
  public long getOpTimeout () {

    return memcachedClient.getOpTimeout();
  }

  @Override
  public <T> ProxyCASResponse<T> createCASResponse (long cas, T value) {

    return new XMemcachedCASResponse<>(new GetsResponse<>(cas, value));
  }

  @Override
  public <T> T get (String key)
    throws TimeoutException, InterruptedException, MemcachedException {

    return memcachedClient.get(key);
  }

  @Override
  public <T> Map<String, T> get (Collection<String> keys)
    throws TimeoutException, InterruptedException, MemcachedException {

    return memcachedClient.get(keys);
  }

  @Override
  public <T> ProxyCASResponse<T> casGet (String key)
    throws TimeoutException, InterruptedException, MemcachedException {

    GetsResponse<T> getsResponse;

    if ((getsResponse = memcachedClient.<T>gets(key)) == null) {

      return null;
    }

    return new XMemcachedCASResponse<>(getsResponse);
  }

  @Override
  public <T> boolean set (String key, int expiration, T value)
    throws TimeoutException, InterruptedException, MemcachedException {

    return memcachedClient.set(key, expiration, value);
  }

  @Override
  public <T> boolean casSet (String key, int expiration, T value, long cas)
    throws TimeoutException, InterruptedException, MemcachedException {

    return memcachedClient.cas(key, expiration, value, cas);
  }

  @Override
  public boolean delete (String key)
    throws TimeoutException, InterruptedException, MemcachedException {

    return memcachedClient.delete(key);
  }

  @Override
  public boolean casDelete (String key, long cas)
    throws TimeoutException, InterruptedException, MemcachedException {

    return memcachedClient.delete(key, cas);
  }

  @Override
  public boolean touch (String key, int expiration)
    throws TimeoutException, InterruptedException, MemcachedException {

    return memcachedClient.touch(key, expiration);
  }

  @Override
  public <T> T getAndTouch (String key, int expiration)
    throws TimeoutException, InterruptedException, MemcachedException {

    return memcachedClient.getAndTouch(key, expiration);
  }

  @Override
  public void clear ()
    throws TimeoutException, InterruptedException, MemcachedException {

    memcachedClient.flushAll();
  }

  @Override
  public void shutdown ()
    throws IOException {

    memcachedClient.shutdown();
  }
}