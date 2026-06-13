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
package org.smallmind.web.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.smallmind.web.jetty.installer.FilterInstaller;
import org.smallmind.web.jetty.installer.ListenerInstaller;
import org.smallmind.web.jetty.installer.ServletInstaller;
import org.smallmind.web.jetty.installer.WebServiceInstaller;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit coverage for the dependency-light support types in the {@code jetty} root package: the in-memory
 * {@link ByteArrayResource}, the SSL configuration holders, the {@link ServicePath} annotation contract,
 * the {@link JettyInitializationException} constructors, and the {@link JettyWebAppState} accumulator.
 */
@Test(groups = "unit")
public class JettySupportTypesTest {

  @ServicePath(value = "/quote", context = "/finance")
  public static class AnnotatedService {

  }

  public void testByteArrayResourceFlags () {

    ByteArrayResource resource = new ByteArrayResource(new byte[] {1, 2, 3});

    Assert.assertTrue(resource.isReadable());
    Assert.assertTrue(resource.exists());
    Assert.assertFalse(resource.isDirectory());
    Assert.assertFalse(resource.isContainedIn(resource));
    Assert.assertNull(resource.getPath());
    Assert.assertNull(resource.getURI());
    Assert.assertNull(resource.getName());
    Assert.assertNull(resource.getFileName());
    Assert.assertNull(resource.resolve("/sub"));
    Assert.assertEquals(resource.length(), 3L);
  }

  public void testByteArrayResourceStreamRoundTrip ()
    throws IOException {

    byte[] bytes = "byte-resource".getBytes(StandardCharsets.UTF_8);
    ByteArrayResource resource = new ByteArrayResource(bytes);

    try (InputStream inputStream = resource.newInputStream()) {
      Assert.assertEquals(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), "byte-resource");
    }
  }

  public void testSSLStoreAccessors () {

    SSLStore store = new SSLStore();

    Assert.assertNull(store.getResource());
    Assert.assertNull(store.getPassword());

    store.setResource("file:/tmp/keystore.jks");
    store.setPassword("secret");

    Assert.assertEquals(store.getResource(), "file:/tmp/keystore.jks");
    Assert.assertEquals(store.getPassword(), "secret");
  }

  public void testSSLStoreReadsBytesFromFile ()
    throws IOException, ResourceException {

    Path storeFile = Files.createTempFile("jetty-sslstore", ".bin");

    Files.write(storeFile, "jetty-sslstore-content".getBytes(StandardCharsets.UTF_8));

    try {

      SSLStore store = new SSLStore();

      store.setResource("file:" + storeFile.toAbsolutePath());

      Assert.assertEquals(new String(store.getBytes(), StandardCharsets.UTF_8), "jetty-sslstore-content");
    } finally {
      Files.deleteIfExists(storeFile);
    }
  }

  public void testSSLStoreUnknownSchemeThrowsResourceException () {

    SSLStore store = new SSLStore();

    store.setResource("bogusscheme:/nowhere");

    Assert.assertThrows(ResourceException.class, store::getBytes);
  }

  public void testSSLInfoDefaults () {

    SSLInfo info = new SSLInfo();

    Assert.assertEquals(info.getPort(), 443);
    Assert.assertFalse(info.isRequireClientAuth());
    Assert.assertNull(info.getKeySSLStore());
    Assert.assertNull(info.getTrustSSLStore());
  }

  public void testSSLInfoAccessors () {

    SSLInfo info = new SSLInfo();
    SSLStore keyStore = new SSLStore();
    SSLStore trustStore = new SSLStore();

    info.setPort(8443);
    info.setRequireClientAuth(true);
    info.setKeySSLStore(keyStore);
    info.setTrustSSLStore(trustStore);

    Assert.assertEquals(info.getPort(), 8443);
    Assert.assertTrue(info.isRequireClientAuth());
    Assert.assertSame(info.getKeySSLStore(), keyStore);
    Assert.assertSame(info.getTrustSSLStore(), trustStore);
  }

  public void testServicePathAnnotationValues () {

    ServicePath servicePath = AnnotatedService.class.getAnnotation(ServicePath.class);

    Assert.assertNotNull(servicePath);
    Assert.assertEquals(servicePath.value(), "/quote");
    Assert.assertEquals(servicePath.context(), "/finance");
    Assert.assertSame(servicePath.annotationType(), ServicePath.class);
    Assert.assertTrue(servicePath instanceof Annotation);
  }

  public void testJettyInitializationExceptionFormattedMessage () {

    JettyInitializationException exception = new JettyInitializationException("Bad port(%d)", 70000);

    Assert.assertEquals(exception.getMessage(), "Bad port(70000)");
    Assert.assertNull(exception.getCause());
  }

  public void testJettyInitializationExceptionWrapsCause () {

    IllegalStateException cause = new IllegalStateException("boom");
    JettyInitializationException exception = new JettyInitializationException(cause);

    Assert.assertSame(exception.getCause(), cause);
  }

  public void testJettyWebAppStateStartsEmpty () {

    JettyWebAppState state = new JettyWebAppState();

    Assert.assertTrue(state.getWebServiceInstallerList().isEmpty());
    Assert.assertTrue(state.getListenerInstallerList().isEmpty());
    Assert.assertTrue(state.getFilterInstallerList().isEmpty());
    Assert.assertTrue(state.getServletInstallerList().isEmpty());
  }

  public void testJettyWebAppStateAccumulatesInstallers () {

    JettyWebAppState state = new JettyWebAppState();
    WebServiceInstaller webServiceInstaller = new WebServiceInstaller("/svc", new Object());
    ListenerInstaller listenerInstaller = new ListenerInstaller();
    FilterInstaller filterInstaller = new FilterInstaller();
    ServletInstaller servletInstaller = new ServletInstaller();

    state.addWebServiceInstaller(webServiceInstaller);
    state.addListenerInstaller(listenerInstaller);
    state.addFilterInstaller(filterInstaller);
    state.addServletInstaller(servletInstaller);

    Assert.assertEquals(state.getWebServiceInstallerList().size(), 1);
    Assert.assertSame(state.getWebServiceInstallerList().getFirst(), webServiceInstaller);
    Assert.assertEquals(state.getListenerInstallerList().size(), 1);
    Assert.assertSame(state.getListenerInstallerList().getFirst(), listenerInstaller);
    Assert.assertEquals(state.getFilterInstallerList().size(), 1);
    Assert.assertSame(state.getFilterInstallerList().getFirst(), filterInstaller);
    Assert.assertEquals(state.getServletInstallerList().size(), 1);
    Assert.assertSame(state.getServletInstallerList().getFirst(), servletInstaller);
  }
}
