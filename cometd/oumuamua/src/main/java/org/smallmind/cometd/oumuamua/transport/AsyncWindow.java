package org.smallmind.cometd.oumuamua.transport;

import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

public class AsyncWindow {

  private final ThreadLocal<AsyncContext> asyncContextThreadLocal = new ThreadLocal<>();

  public synchronized void addAsyncContext (AsyncContext asyncContext) {

    asyncContextThreadLocal.set(asyncContext);
  }

  public synchronized void clearAsyncContext () {

    asyncContextThreadLocal.remove();
  }

  public synchronized String getUserAgent () {

    AsyncContext asyncContext;

    return ((asyncContext = asyncContextThreadLocal.get()) == null) ? null : ((HttpServletRequest)asyncContext.getRequest()).getHeader("User-Agent");
  }

  public HttpServletRequest getRequest () {

    AsyncContext asyncContext;

    return ((asyncContext = asyncContextThreadLocal.get()) == null) ? null : (HttpServletRequest)asyncContext.getRequest();
  }
}
