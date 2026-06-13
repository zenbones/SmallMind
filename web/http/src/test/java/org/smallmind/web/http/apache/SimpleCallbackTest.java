/*
 * Copyright (c) 2007 through 2026 David Berkman
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the {@link SimpleCallback}/{@link HttpCallback} state machine in isolation: response and
 * exception capture, cancellation reporting, and the {@link HttpCallback#await} latch behavior.
 */
@Test(groups = "unit")
public class SimpleCallbackTest {

  public void testCompletedResponseIsCaptured ()
    throws Exception {

    SimpleHttpResponse response = SimpleHttpResponse.create(200);
    SimpleCallback callback = new SimpleCallback();

    callback.completed(response);

    Assert.assertSame(callback.getResponse(), response);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testFailedExceptionIsRethrown ()
    throws Exception {

    SimpleCallback callback = new SimpleCallback();

    callback.failed(new IllegalStateException("network down"));
    callback.getResponse();
  }

  @Test(expectedExceptions = HttpRequestCancelledException.class)
  public void testCancellationReportedAsError ()
    throws Exception {

    SimpleCallback callback = new SimpleCallback();

    callback.cancelled();
    callback.getResponse();
  }

  public void testAwaitReturnsAfterCompletion ()
    throws Exception {

    SimpleCallback callback = new SimpleCallback();

    callback.completed(SimpleHttpResponse.create(204));
    callback.await(1, TimeUnit.SECONDS);
  }

  @Test(expectedExceptions = TimeoutException.class)
  public void testAwaitTimesOutBeforeCompletion ()
    throws Exception {

    new SimpleCallback().await(50, TimeUnit.MILLISECONDS);
  }
}
