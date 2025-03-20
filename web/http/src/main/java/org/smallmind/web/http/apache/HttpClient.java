/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.web.http.apache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

public class HttpClient {

  private static final CloseableHttpAsyncClient HTTP_ASYNC_CLIENT;
  private static final int HTTP_CONCURRENCY_LEVEL = 300;

  static {
    try {

      PoolingAsyncClientConnectionManager poolingClientConnectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                                                                             .setDefaultTlsConfig(TlsConfig.custom().setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_1).build())
                                                                             .setTlsStrategy(ClientTlsStrategyBuilder.create().setSslContext(SSLContexts.custom().loadTrustMaterial((chain, authType) -> true).build()).build())
                                                                             .build();

      poolingClientConnectionManager.setDefaultMaxPerRoute(HTTP_CONCURRENCY_LEVEL);
      poolingClientConnectionManager.setMaxTotal(HTTP_CONCURRENCY_LEVEL);

      HTTP_ASYNC_CLIENT = HttpAsyncClientBuilder.create()
                            .setIOReactorConfig(IOReactorConfig.custom().setSoTimeout(Timeout.of(5, TimeUnit.SECONDS)).build())
                            .setConnectionManager(poolingClientConnectionManager)
                            .build();
      HTTP_ASYNC_CLIENT.start();
    } catch (Exception exception) {
      throw new StaticInitializationError(exception);
    }
  }

  public static void execute (SimpleHttpRequest httpRequest, HttpCallback<SimpleHttpResponse> callback, int timeoutSeconds)
    throws InterruptedException, ExecutionException, TimeoutException {

    long start = System.currentTimeMillis();

    HTTP_ASYNC_CLIENT.execute(httpRequest, callback).get(timeoutSeconds, TimeUnit.SECONDS);
    callback.await((timeoutSeconds * 1000L) - (System.currentTimeMillis() - start), TimeUnit.MILLISECONDS);
  }
}
