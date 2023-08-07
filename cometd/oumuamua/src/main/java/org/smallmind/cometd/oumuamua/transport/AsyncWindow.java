package org.smallmind.cometd.oumuamua.transport;

import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

public class AsyncWindow {

  private final AtomicReference<AsyncContext> asyncContextRef = new AtomicReference<>();

  public synchronized void addAsyncContext (AsyncContext asyncContext) {

    asyncContextRef.set(asyncContext);
  }

  public synchronized void clearAsyncContext () {

    asyncContextRef.set(null);
  }

  public synchronized String getUserAgent () {

    AsyncContext asyncContext;

    return ((asyncContext = asyncContextRef.get()) == null) ? null : ((HttpServletRequest)asyncContext.getRequest()).getHeader("User-Agent");
  }

  public HttpServletRequest getRequest () {

    AsyncContext asyncContext;

    return ((asyncContext = asyncContextRef.get()) == null) ? null : (HttpServletRequest)asyncContext.getRequest();
  }
}
