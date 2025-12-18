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
 * Aggregates all configuration options for a single Jetty web application context.
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
   * Retrieves the configuration for serving classpath resources.
   *
   * @return the class loader resource option or {@code null} if disabled
   */
  public ClassLoaderResourceOption getClassLoaderResourceOption () {

    return classLoaderResourceOption;
  }

  /**
   * Sets the configuration for serving classpath resources.
   *
   * @param classLoaderResourceOption the class loader resource option
   */
  public void setClassLoaderResourceOption (ClassLoaderResourceOption classLoaderResourceOption) {

    this.classLoaderResourceOption = classLoaderResourceOption;
  }

  /**
   * Retrieves the configuration for serving filesystem document roots.
   *
   * @return the document root option or {@code null} if none
   */
  public DocumentRootOption getDocumentRootOption () {

    return documentRootOption;
  }

  /**
   * Sets the configuration for serving filesystem document roots.
   *
   * @param documentRootOption the document root option
   */
  public void setDocumentRootOption (DocumentRootOption documentRootOption) {

    this.documentRootOption = documentRootOption;
  }

  /**
   * Retrieves the JAX-RS configuration for the context.
   *
   * @return JAX-RS option or {@code null} if REST is not exposed
   */
  public JaxRSOption getJaxRSOption () {

    return jaxRSOption;
  }

  /**
   * Sets the JAX-RS configuration for the context.
   *
   * @param jaxRSOption JAX-RS option
   */
  public void setJaxRSOption (JaxRSOption jaxRSOption) {

    this.jaxRSOption = jaxRSOption;
  }

  /**
   * Retrieves the Spring support option, if enabled.
   *
   * @return Spring support option or {@code null} if not requested
   */
  public SpringSupportOption getSpringSupportOption () {

    return springSupportOption;
  }

  /**
   * Sets the Spring support option used to enable request context wiring.
   *
   * @param springSupportOption Spring support option
   */
  public void setSpringSupportOption (SpringSupportOption springSupportOption) {

    this.springSupportOption = springSupportOption;
  }

  /**
   * Retrieves the WebSocket configuration for this context.
   *
   * @return WebSocket option or {@code null} if WebSockets are not configured
   */
  public WebSocketOption getWebSocketOption () {

    return webSocketOption;
  }

  /**
   * Sets the WebSocket configuration for this context.
   *
   * @param webSocketOption WebSocket option
   */
  public void setWebSocketOption (WebSocketOption webSocketOption) {

    this.webSocketOption = webSocketOption;
  }

  /**
   * Retrieves the context path under which the web application will be served.
   *
   * @return the context path
   */
  public String getContextPath () {

    return contextPath;
  }

  /**
   * Sets the context path under which the web application will be served.
   *
   * @param contextPath the context path
   */
  public void setContextPath (String contextPath) {

    this.contextPath = contextPath;
  }

  /**
   * Retrieves the base path used for SOAP web services in this context.
   *
   * @return SOAP path segment
   */
  public String getSoapPath () {

    return soapPath;
  }

  /**
   * Sets the base path used for SOAP web services in this context.
   *
   * @param soapPath SOAP path segment
   */
  public void setSoapPath (String soapPath) {

    this.soapPath = soapPath;
  }
}
