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
package org.smallmind.phalanx.wire;

import java.lang.reflect.Method;
import java.util.Map;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.WireContext;
import org.smallmind.phalanx.wire.transport.RequestTransport;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the client-side proxy dispatch in {@link WireInvocationHandler}: how each dispatch
 * annotation selects a {@link Voice}, how the effective timeout is resolved, and the fail-fast
 * validation paths. A capturing {@link RequestTransport} records the {@link Voice}/{@link Route}
 * the handler produces, so no broker is involved.
 */
@Test(groups = "unit")
public class WireInvocationHandlerTest {

  public interface VoiceService {

    @InOut(timeoutSeconds = 5)
    String talk (@Argument("a") String a);

    @InOnly
    void fireAndForget (@Argument("a") String a);

    @Whisper(timeoutSeconds = 7)
    String whisper (@Argument("a") String a);

    @Shout
    void shout (@Argument("a") String a);

    @InOnly
    String badInOnlyReturnsValue (@Argument("a") String a);

    @InOnly
    void badInOnlyDeclaresThrows (@Argument("a") String a)
      throws Exception;

    String plain (@Argument("a") String a);
  }

  private WireInvocationHandler handler (ParameterExtractor<String> serviceGroupExtractor, ParameterExtractor<String> instanceIdExtractor, ParameterExtractor<Long> timeoutExtractor, RequestTransport transport)
    throws Exception {

    return new WireInvocationHandler(transport, 1, "VoiceService", VoiceService.class, serviceGroupExtractor, instanceIdExtractor, timeoutExtractor);
  }

  private Method method (String name)
    throws NoSuchMethodException {

    return VoiceService.class.getMethod(name, String.class);
  }

  @Test
  public void testInOutSelectsTwoWayTalkingVoice ()
    throws Throwable {

    CapturingRequestTransport transport = new CapturingRequestTransport();

    handler(new StaticParameterExtractor<>("group"), null, null, transport).invoke(null, method("talk"), new Object[] {"x"});

    Assert.assertTrue(transport.voice instanceof Talking);
    Assert.assertEquals(transport.voice.getMode(), VocalMode.TALK);
    Assert.assertEquals(transport.voice.getConversation().getConversationType(), ConversationType.IN_OUT);
    Assert.assertEquals(((TwoWayConversation)transport.voice.getConversation()).getTimeout(), (Long)5L);
    Assert.assertEquals(transport.voice.getServiceGroup(), "group");
    Assert.assertEquals(transport.route.getService(), "VoiceService");
    Assert.assertEquals(transport.route.getVersion(), 1);
    Assert.assertEquals(transport.route.getFunction().getName(), "talk");
  }

  @Test
  public void testInOnlySelectsOneWayTalkingVoice ()
    throws Throwable {

    CapturingRequestTransport transport = new CapturingRequestTransport();

    handler(new StaticParameterExtractor<>("group"), null, null, transport).invoke(null, method("fireAndForget"), new Object[] {"x"});

    Assert.assertTrue(transport.voice instanceof Talking);
    Assert.assertEquals(transport.voice.getConversation().getConversationType(), ConversationType.IN_ONLY);
  }

  @Test
  public void testShoutSelectsShoutingVoice ()
    throws Throwable {

    CapturingRequestTransport transport = new CapturingRequestTransport();

    handler(new StaticParameterExtractor<>("group"), null, null, transport).invoke(null, method("shout"), new Object[] {"x"});

    Assert.assertTrue(transport.voice instanceof Shouting);
    Assert.assertEquals(transport.voice.getMode(), VocalMode.SHOUT);
    Assert.assertEquals(transport.voice.getServiceGroup(), "group");
  }

  @Test
  public void testWhisperSelectsWhisperingVoiceWithInstanceAndTimeout ()
    throws Throwable {

    CapturingRequestTransport transport = new CapturingRequestTransport();

    handler(new StaticParameterExtractor<>("group"), new StaticParameterExtractor<>("instance-1"), null, transport).invoke(null, method("whisper"), new Object[] {"x"});

    Assert.assertTrue(transport.voice instanceof Whispering);
    Assert.assertEquals(transport.voice.getMode(), VocalMode.WHISPER);
    Assert.assertEquals(transport.voice.getInstanceId(), "instance-1");
    Assert.assertEquals(((TwoWayConversation)transport.voice.getConversation()).getTimeout(), (Long)7L);
  }

  @Test
  public void testTimeoutExtractorOverridesAnnotation ()
    throws Throwable {

    CapturingRequestTransport transport = new CapturingRequestTransport();

    handler(new StaticParameterExtractor<>("group"), null, new StaticParameterExtractor<>(99L), transport).invoke(null, method("talk"), new Object[] {"x"});

    Assert.assertEquals(((TwoWayConversation)transport.voice.getConversation()).getTimeout(), (Long)99L);
  }

  @Test(expectedExceptions = ServiceDefinitionException.class)
  public void testNullServiceGroupExtractorIsRejectedAtConstruction ()
    throws Exception {

    handler(null, null, null, new CapturingRequestTransport());
  }

  @Test(expectedExceptions = ServiceDefinitionException.class)
  public void testWhisperWithoutInstanceExtractorIsRejected ()
    throws Throwable {

    handler(new StaticParameterExtractor<>("group"), null, null, new CapturingRequestTransport()).invoke(null, method("whisper"), new Object[] {"x"});
  }

  @Test(expectedExceptions = ServiceDefinitionException.class)
  public void testInOnlyReturningNonVoidIsRejected ()
    throws Throwable {

    handler(new StaticParameterExtractor<>("group"), null, null, new CapturingRequestTransport()).invoke(null, method("badInOnlyReturnsValue"), new Object[] {"x"});
  }

  @Test(expectedExceptions = ServiceDefinitionException.class)
  public void testInOnlyDeclaringThrowsIsRejected ()
    throws Throwable {

    handler(new StaticParameterExtractor<>("group"), null, null, new CapturingRequestTransport()).invoke(null, method("badInOnlyDeclaresThrows"), new Object[] {"x"});
  }

  @Test(expectedExceptions = ServiceDefinitionException.class)
  public void testArgumentCountMismatchIsRejected ()
    throws Throwable {

    handler(new StaticParameterExtractor<>("group"), null, null, new CapturingRequestTransport()).invoke(null, method("talk"), new Object[0]);
  }

  @Test(expectedExceptions = MissingInvocationException.class)
  public void testUnknownMethodIsRejected ()
    throws Throwable {

    handler(new StaticParameterExtractor<>("group"), null, null, new CapturingRequestTransport()).invoke(null, Comparable.class.getMethod("compareTo", Object.class), new Object[] {"x"});
  }

  @Test
  public void testTimeoutExtractorOverridesWhisperAnnotation ()
    throws Throwable {

    CapturingRequestTransport transport = new CapturingRequestTransport();

    handler(new StaticParameterExtractor<>("group"), new StaticParameterExtractor<>("instance-1"), new StaticParameterExtractor<>(42L), transport).invoke(null, method("whisper"), new Object[] {"x"});

    Assert.assertEquals(((TwoWayConversation)transport.voice.getConversation()).getTimeout(), (Long)42L);
  }

  @Test
  public void testUnannotatedMethodDefaultsToInOutWithNoTimeout ()
    throws Throwable {

    CapturingRequestTransport transport = new CapturingRequestTransport();

    handler(new StaticParameterExtractor<>("group"), null, null, transport).invoke(null, method("plain"), new Object[] {"x"});

    Assert.assertTrue(transport.voice instanceof Talking);
    Assert.assertEquals(transport.voice.getConversation().getConversationType(), ConversationType.IN_OUT);
    Assert.assertNull(((TwoWayConversation)transport.voice.getConversation()).getTimeout());
  }

  @Test
  public void testActiveWireContextsArePassedToTransport ()
    throws Throwable {

    CapturingRequestTransport transport = new CapturingRequestTransport();

    ContextFactory.pushContext(new TestWireContext("flibble"));
    try {
      handler(new StaticParameterExtractor<>("group"), null, null, transport).invoke(null, method("talk"), new Object[] {"x"});
    } finally {
      ContextFactory.popContext(TestWireContext.class);
    }

    Assert.assertEquals(transport.contexts.length, 1);
    Assert.assertTrue(transport.contexts[0] instanceof TestWireContext);
  }

  private static class CapturingRequestTransport implements RequestTransport {

    private Voice<?, ?> voice;
    private Route route;
    private Map<String, Object> arguments;
    private WireContext[] contexts;

    @Override
    public String getCallerId () {

      return "caller";
    }

    @Override
    public Object transmit (Voice<?, ?> voice, Route route, Map<String, Object> arguments, WireContext... contexts) {

      this.voice = voice;
      this.route = route;
      this.arguments = arguments;
      this.contexts = contexts;

      return "RESULT";
    }

    @Override
    public void completeCallback (String correlationId, ResultSignal resultSignal) {

    }

    @Override
    public void close () {

    }
  }
}
