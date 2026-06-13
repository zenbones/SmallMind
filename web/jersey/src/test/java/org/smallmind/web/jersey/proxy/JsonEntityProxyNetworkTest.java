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
package org.smallmind.web.jersey.proxy;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.Path;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.scribe.pen.Level;
import org.smallmind.web.jersey.aop.EntityParam;
import org.smallmind.web.jersey.aop.Envelope;
import org.smallmind.web.json.scaffold.util.JsonCodec;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration test that drives the full {@link JsonEntityResourceProxyFactory} proxy path against an in-process JDK
 * {@link HttpServer} bound to an ephemeral port (no external services), reusing the harness pattern from
 * {@link JsonTargetNetworkTest}. A proxy is generated for a small resource interface whose parameters carry
 * {@link EntityParam} (and, for one parameter, an {@link XmlJavaTypeAdapter}) and is invoked end-to-end. The test
 * asserts the POSTed URL composition ({@code basePath} = {@code /}{@code versionPrefix}{@code version}{@code /}{@code serviceName}
 * followed by the method name or its {@link Path}), the JSON {@link org.smallmind.web.jersey.aop.Envelope} body of named
 * {@link org.smallmind.web.jersey.aop.Argument}s, the {@link jakarta.xml.bind.annotation.adapters.XmlAdapter}-marshalled
 * argument transformation, an injected request header, and deserialization of the JSON response into the declared return
 * type. It also covers the per-method {@code jsonArgument} caching (same method invoked twice) and the negative path
 * where a parameter missing {@link EntityParam} raises a {@link ResourceDefinitionException}.
 *
 * <p>The {@link StringToLengthAdapter} marshals an {@link Integer} to a string of that many {@code x} characters, so a
 * {@code size} argument of {@code 3} arrives in the envelope as {@code "xxx"}, exercising the adapter-marshalling branch
 * of {@link JsonEntityInvocationHandler}.
 */
@Test(groups = "integration")
public class JsonEntityProxyNetworkTest {

  private final AtomicReference<RecordedRequest> lastRequestRef = new AtomicReference<>();
  private HttpServer server;
  private int port;

  public static class Echo {

    private String name;
    private int count;

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }

    public int getCount () {

      return count;
    }

    public void setCount (int count) {

      this.count = count;
    }
  }

  public interface SampleResource {

    Echo lookup (@EntityParam("id") String id, @XmlJavaTypeAdapter(StringToLengthAdapter.class) @EntityParam("size") Integer size);

    @Path("/custom-path")
    Echo annotated (@EntityParam("id") String id);
  }

  public interface BrokenResource {

    Echo broken (String value);
  }

  /**
   * Test {@link jakarta.xml.bind.annotation.adapters.XmlAdapter} that marshals an {@link Integer} to a string of that
   * many {@code x} characters. Declared with a public no-arg constructor so the production code can instantiate it
   * reflectively.
   */
  public static class StringToLengthAdapter extends jakarta.xml.bind.annotation.adapters.XmlAdapter<String, Integer> {

    @Override
    public Integer unmarshal (String value) {

      return (value == null) ? null : value.length();
    }

    @Override
    public String marshal (Integer value) {

      return (value == null) ? null : "x".repeat(value);
    }
  }

  private static class MarkerHeaderInjector implements JsonHeaderInjector {

    @Override
    public JsonHeader injectOnInvoke (Object proxy, Method method, Object[] args) {

      return new JsonHeader("X-Marker", "marker-value");
    }
  }

  private record RecordedRequest(String method, String uri, String body, String marker) {

  }

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", this::handleEcho);
    server.start();
    port = server.getAddress().getPort();
  }

  @AfterClass(alwaysRun = true)
  public void afterClass () {

    if (server != null) {
      server.stop(0);
    }
  }

  @BeforeMethod
  public void beforeMethod () {

    new PerApplicationContext();
    lastRequestRef.set(null);
  }

  private void handleEcho (HttpExchange exchange)
    throws IOException {

    byte[] requestBytes = exchange.getRequestBody().readAllBytes();

    lastRequestRef.set(new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI().toString(), new String(requestBytes, StandardCharsets.UTF_8), exchange.getRequestHeaders().getFirst("X-Marker")));

    byte[] body = "{\"name\":\"echoed\",\"count\":7}".getBytes(StandardCharsets.UTF_8);

    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, body.length);
    try (OutputStream outputStream = exchange.getResponseBody()) {
      outputStream.write(body);
    }
  }

  private SampleResource generate (JsonHeaderInjector... headerInjectors)
    throws Exception {

    JsonTarget target = JsonTargetFactory.manufacture(HttpProtocol.HTTP, "127.0.0.1", port);

    return (SampleResource)JsonEntityResourceProxyFactory.generateProxy(target, "v", 1, "sample-service", SampleResource.class, Level.OFF, headerInjectors);
  }

  public void testProxyInvocationComposesUrlAndEnvelopeAndMarshalsAdapterArgument ()
    throws Exception {

    SampleResource resource = generate();
    Echo response = resource.lookup("abc", 3);

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getName(), "echoed");
    Assert.assertEquals(response.getCount(), 7);

    RecordedRequest recordedRequest = lastRequestRef.get();

    Assert.assertEquals(recordedRequest.method(), "POST");
    Assert.assertEquals(recordedRequest.uri(), "/v1/sample-service/lookup");

    Envelope envelope = JsonCodec.read(recordedRequest.body().getBytes(StandardCharsets.UTF_8), Envelope.class);

    Assert.assertEquals(envelope.get("id"), "abc");
    // The adapter marshals the Integer 3 into a string of 3 'x' characters before it is enveloped.
    Assert.assertEquals(envelope.get("size"), "xxx");
  }

  public void testProxyInvocationHonorsPathAnnotation ()
    throws Exception {

    SampleResource resource = generate();
    Echo response = resource.annotated("xyz");

    Assert.assertNotNull(response);

    RecordedRequest recordedRequest = lastRequestRef.get();

    Assert.assertEquals(recordedRequest.uri(), "/v1/sample-service/custom-path");

    Envelope envelope = JsonCodec.read(recordedRequest.body().getBytes(StandardCharsets.UTF_8), Envelope.class);

    Assert.assertEquals(envelope.get("id"), "xyz");
  }

  public void testProxyInvocationAppliesHeaderInjector ()
    throws Exception {

    SampleResource resource = generate(new MarkerHeaderInjector());

    resource.annotated("xyz");

    Assert.assertEquals(lastRequestRef.get().marker(), "marker-value");
  }

  public void testNullHeaderInjectorAddsNoHeader ()
    throws Exception {

    SampleResource resource = generate((proxy, method, args) -> null);

    resource.annotated("xyz");

    Assert.assertNull(lastRequestRef.get().marker());
  }

  public void testJsonArgumentCachingAcrossRepeatedInvocations ()
    throws Exception {

    SampleResource resource = generate();

    resource.lookup("first", 1);
    Envelope firstEnvelope = JsonCodec.read(lastRequestRef.get().body().getBytes(StandardCharsets.UTF_8), Envelope.class);

    resource.lookup("second", 2);
    Envelope secondEnvelope = JsonCodec.read(lastRequestRef.get().body().getBytes(StandardCharsets.UTF_8), Envelope.class);

    Assert.assertEquals(firstEnvelope.get("id"), "first");
    Assert.assertEquals(firstEnvelope.get("size"), "x");
    Assert.assertEquals(secondEnvelope.get("id"), "second");
    Assert.assertEquals(secondEnvelope.get("size"), "xx");
  }

  public void testMissingEntityParamThrowsResourceDefinitionException ()
    throws Exception {

    JsonTarget target = JsonTargetFactory.manufacture(HttpProtocol.HTTP, "127.0.0.1", port);
    BrokenResource resource = (BrokenResource)JsonEntityResourceProxyFactory.generateProxy(target, "v", 1, "broken-service", BrokenResource.class, Level.OFF);

    try {
      resource.broken("value");
      Assert.fail("Expected a ResourceDefinitionException");
    } catch (UndeclaredThrowableException undeclaredThrowableException) {
      // The interface method declares no checked exceptions, so the JDK proxy wraps the checked
      // ResourceDefinitionException raised by argument construction in an UndeclaredThrowableException.
      Assert.assertTrue(undeclaredThrowableException.getCause() instanceof ResourceDefinitionException);
      Assert.assertTrue(undeclaredThrowableException.getCause().getMessage().contains("broken-service"));
    }
  }
}
