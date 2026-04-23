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
package org.smallmind.web.grizzly.option;

/**
 * Aggregates all feature options for a single Grizzly servlet context, including static resources, JAX-RS, SOAP,
 * Spring support, and WebSocket.
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
   * Returns the option for serving static resources from the classpath.
   *
   * @return class loader resource option, or {@code null} if not configured
   */
  public ClassLoaderResourceOption getClassLoaderResourceOption () {

    return classLoaderResourceOption;
  }

  /**
   * Sets the option for serving static resources from the classpath.
   *
   * @param classLoaderResourceOption static class loader resource configuration
   */
  public void setClassLoaderResourceOption (ClassLoaderResourceOption classLoaderResourceOption) {

    this.classLoaderResourceOption = classLoaderResourceOption;
  }

  /**
   * Returns the option for serving static files from the filesystem.
   *
   * @return document root option, or {@code null} if not configured
   */
  public DocumentRootOption getDocumentRootOption () {

    return documentRootOption;
  }

  /**
   * Sets the option for serving static files from the filesystem.
   *
   * @param documentRootOption filesystem document root configuration
   */
  public void setDocumentRootOption (DocumentRootOption documentRootOption) {

    this.documentRootOption = documentRootOption;
  }

  /**
   * Returns the JAX-RS configuration for the Jersey servlet mapping.
   *
   * @return JAX-RS option, or {@code null} if REST is not enabled
   */
  public JaxRSOption getJaxRSOption () {

    return jaxRSOption;
  }

  /**
   * Sets the JAX-RS configuration for the Jersey servlet mapping.
   *
   * @param jaxRSOption JAX-RS REST path configuration
   */
  public void setJaxRSOption (JaxRSOption jaxRSOption) {

    this.jaxRSOption = jaxRSOption;
  }

  /**
   * Returns the Spring support marker option.
   *
   * @return Spring support option, or {@code null} if Spring request context is not enabled
   */
  public SpringSupportOption getSpringSupportOption () {

    return springSupportOption;
  }

  /**
   * Sets the Spring support marker option.
   *
   * @param springSupportOption marker enabling Spring request context listener registration
   */
  public void setSpringSupportOption (SpringSupportOption springSupportOption) {

    this.springSupportOption = springSupportOption;
  }

  /**
   * Returns the WebSocket deployment option.
   *
   * @return WebSocket option, or {@code null} if WebSocket is not enabled
   */
  public WebSocketOption getWebSocketOption () {

    return webSocketOption;
  }

  /**
   * Sets the WebSocket deployment option.
   *
   * @param webSocketOption WebSocket feature configuration
   */
  public void setWebSocketOption (WebSocketOption webSocketOption) {

    this.webSocketOption = webSocketOption;
  }

  /**
   * Returns the servlet context path for this application.
   *
   * @return context path
   */
  public String getContextPath () {

    return contextPath;
  }

  /**
   * Sets the servlet context path for this application.
   *
   * @param contextPath servlet context path
   */
  public void setContextPath (String contextPath) {

    this.contextPath = contextPath;
  }

  /**
   * Returns the base URI path under which SOAP services will be exposed.
   *
   * @return SOAP base path
   */
  public String getSoapPath () {

    return soapPath;
  }

  /**
   * Sets the base URI path under which SOAP services will be exposed.
   *
   * @param soapPath SOAP base path
   */
  public void setSoapPath (String soapPath) {

    this.soapPath = soapPath;
  }
}
