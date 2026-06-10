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
package org.smallmind.phalanx.wire.transport;

import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.phalanx.wire.Color;
import org.smallmind.phalanx.wire.StaticParameterExtractor;
import org.smallmind.phalanx.wire.TestWireContext;
import org.smallmind.phalanx.wire.WireContextManager;
import org.smallmind.phalanx.wire.WireTestingException;
import org.smallmind.phalanx.wire.WireTestingService;
import org.smallmind.phalanx.wire.WireTestingServiceImpl;
import org.smallmind.phalanx.wire.spring.WireProxyFactory;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.smallmind.testbench.logger.TestLoggerConfiguration;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Shared end-to-end contract that every broker transport pair must satisfy: a talk/request-reply
 * round trip over the wire, {@link org.smallmind.phalanx.wire.signal.WireContext} propagation,
 * server-side exception propagation, and the response transport's pause/resume lifecycle.
 *
 * <p>Concrete subclasses supply the transport pair via {@link #createResponseTransport} and
 * {@link #createRequestTransport}, and (for an in-process broker) override
 * {@link #prepareInfrastructure}/{@link #teardownInfrastructure}. Docker-backed subclasses pass
 * the relevant {@link DockerApplication} to {@code super(...)}; an in-process subclass passes none.
 */
@Test(groups = "integration")
public abstract class AbstractWireTransportContractTest extends AbstractGroundwaterTest {

  protected RequestTransport requestTransport;
  protected ResponseTransport responseTransport;
  protected WireTestingService wireTestingService;

  protected AbstractWireTransportContractTest (DockerApplication... dockerApplications) {

    super(dockerApplications);
  }

  protected abstract ResponseTransport createResponseTransport ()
    throws Exception;

  protected abstract RequestTransport createRequestTransport ()
    throws Exception;

  protected void prepareInfrastructure ()
    throws Exception {

  }

  protected void teardownInfrastructure ()
    throws Exception {

  }

  protected void warmUp ()
    throws Exception {

  }

  @BeforeClass
  @Override
  public void beforeClass ()
    throws Exception {

    TestLoggerConfiguration.setup();
    super.beforeClass();
    prepareInfrastructure();

    //  Establish the per-application context this thread (and the inheriting worker threads) needs
    //  before any WireContext handle can be registered; the Spring-wired tests get this from foundation.xml.
    new PerApplicationContext();

    WireContextManager.register("test", TestWireContext.class);

    responseTransport = createResponseTransport();

    String instanceId = responseTransport.register(WireTestingService.class, new WireTestingServiceImpl());

    requestTransport = createRequestTransport();
    wireTestingService = (WireTestingService)WireProxyFactory.generateProxy(requestTransport, 1, "WireTestService", WireTestingService.class, new StaticParameterExtractor<>("default"), new StaticParameterExtractor<>(instanceId), null);

    warmUp();
  }

  @AfterClass
  @Override
  public void afterClass ()
    throws Exception {

    if (requestTransport != null) {
      requestTransport.close();
    }
    if (responseTransport != null) {
      responseTransport.close();
    }

    teardownInfrastructure();
    super.afterClass();
  }

  @Test
  public void testEchoRoundTrip () {

    Color[] colors = new Color[] {new Color("red"), new Color("white"), new Color("blue")};

    Assert.assertEquals(wireTestingService.echoString("The quick brown fox"), "The quick brown fox");
    Assert.assertEquals(wireTestingService.echoInt(4291), 4291);
    Assert.assertEquals(wireTestingService.addNumbers(7, 8), (Integer)15);
    Assert.assertEquals(wireTestingService.echoColors(colors), colors);
  }

  @Test
  public void testContextPropagation () {

    ContextFactory.pushContext(new TestWireContext("flibble"));
    try {
      Assert.assertTrue(wireTestingService.hasContext());
    } finally {
      ContextFactory.popContext(TestWireContext.class);
    }
  }

  @Test
  public void testFaultPropagation () {

    WireTestingException capturedException = null;

    try {
      wireTestingService.throwError();
    } catch (WireTestingException wireTestingException) {
      capturedException = wireTestingException;
    }

    Assert.assertNotNull(capturedException);
  }

  @Test
  public void testPauseAndResumeLifecycle ()
    throws Exception {

    Assert.assertEquals(responseTransport.getState(), TransportState.PLAYING);

    responseTransport.pause();
    Assert.assertEquals(responseTransport.getState(), TransportState.PAUSED);

    responseTransport.play();
    Assert.assertEquals(responseTransport.getState(), TransportState.PLAYING);
  }
}
