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
package org.smallmind.web.jetty.option;

/**
 * Aggregates all optional feature configuration for a single Jetty web application context, including static resources, REST, SOAP, WebSocket, and Spring support.
 */
public class WebApplicationOption {

  private ClassLoaderResourceOption classLoaderResourceOption;
  private DocumentRootOption documentRootOption;
  private JaxRSOption jaxRSOption;
  private SpringSupportOption springSupportOption;
  private WebSocketOption webSocketOption;
  private String contextPath = "/context";
  private String soapPath = "/soap";

  /**
   * Returns the classpath-resource serving option, or {@code null} if disabled.
   *
   * @return classpath resource option
   */
  public ClassLoaderResourceOption getClassLoaderResourceOption () {

    return classLoaderResourceOption;
  }

  /**
   * Sets the option for serving static resources from the classpath.
   *
   * @param classLoaderResourceOption classpath resource configuration
   */
  public void setClassLoaderResourceOption (ClassLoaderResourceOption classLoaderResourceOption) {

    this.classLoaderResourceOption = classLoaderResourceOption;
  }

  /**
   * Returns the filesystem document-root serving option, or {@code null} if disabled.
   *
   * @return document root option
   */
  public DocumentRootOption getDocumentRootOption () {

    return documentRootOption;
  }

  /**
   * Sets the option for serving static files from filesystem document roots.
   *
   * @param documentRootOption document root configuration
   */
  public void setDocumentRootOption (DocumentRootOption documentRootOption) {

    this.documentRootOption = documentRootOption;
  }

  /**
   * Returns the JAX-RS configuration for this context, or {@code null} if REST is not enabled.
   *
   * @return JAX-RS option
   */
  public JaxRSOption getJaxRSOption () {

    return jaxRSOption;
  }

  /**
   * Sets the JAX-RS configuration that causes a Jersey servlet to be installed.
   *
   * @param jaxRSOption JAX-RS configuration
   */
  public void setJaxRSOption (JaxRSOption jaxRSOption) {

    this.jaxRSOption = jaxRSOption;
  }

  /**
   * Returns the Spring support option, or {@code null} if Spring request-context wiring is not requested.
   *
   * @return Spring support option
   */
  public SpringSupportOption getSpringSupportOption () {

    return springSupportOption;
  }

  /**
   * Sets the marker option that causes a Spring request-context listener to be registered.
   *
   * @param springSupportOption Spring support marker
   */
  public void setSpringSupportOption (SpringSupportOption springSupportOption) {

    this.springSupportOption = springSupportOption;
  }

  /**
   * Returns the WebSocket configuration for this context, or {@code null} if WebSockets are not enabled.
   *
   * @return WebSocket option
   */
  public WebSocketOption getWebSocketOption () {

    return webSocketOption;
  }

  /**
   * Sets the WebSocket configuration for this context.
   *
   * @param webSocketOption WebSocket configuration
   */
  public void setWebSocketOption (WebSocketOption webSocketOption) {

    this.webSocketOption = webSocketOption;
  }

  /**
   * Returns the context path under which this web application is mounted.
   *
   * @return the context path
   */
  public String getContextPath () {

    return contextPath;
  }

  /**
   * Sets the context path under which this web application will be mounted.
   *
   * @param contextPath the context path
   */
  public void setContextPath (String contextPath) {

    this.contextPath = contextPath;
  }

  /**
   * Returns the path segment appended to the context path for SOAP service endpoints.
   *
   * @return the SOAP sub-path
   */
  public String getSoapPath () {

    return soapPath;
  }

  /**
   * Sets the path segment used to prefix all SOAP service endpoints within this context.
   *
   * @param soapPath the SOAP sub-path
   */
  public void setSoapPath (String soapPath) {

    this.soapPath = soapPath;
  }
}
