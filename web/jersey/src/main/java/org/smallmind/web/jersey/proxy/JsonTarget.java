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
import java.net.URISyntaxException;
import jakarta.ws.rs.WebApplicationException;
import org.apache.hc.client5.http.async.methods.SimpleBody;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.Method;
import org.smallmind.nutsnbolts.http.HttpMethod;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.Pair;
import org.smallmind.nutsnbolts.util.Tuple;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.http.apache.HttpClient;
import org.smallmind.web.http.apache.SimpleCallback;
import org.smallmind.web.json.scaffold.util.JsonCodec;

/**
 * Fluent client wrapper that sends JSON HTTP requests to a fixed host and deserializes the responses.
 */
public class JsonTarget {

  private final HttpHost httpHost;
  private Tuple<String, String> headers;
  private Tuple<String, String> queryParameters;
  private Level level = Level.OFF;
  private String path;

  /**
   * Constructs a target that resolves the host from the given URI string.
   *
   * @param host full URI string (scheme + host, e.g. {@code "https://api.example.com"})
   * @throws URISyntaxException if the URI cannot be parsed
   */
  public JsonTarget (String host)
    throws URISyntaxException {

    httpHost = HttpHost.create(host);
  }

  protected JsonTarget (HttpHost httpHost, String path) {

    this.httpHost = httpHost;
    this.path = path;
  }

  /**
   * Returns a new {@link JsonTarget} for the same host whose path is the given segment joined to this target's
   * existing path. When this target has no path, or only a blank one, the segment is used as-is; otherwise the segment
   * is appended to the existing path so that a context prefix supplied at construction carries through to the request.
   * A single trailing {@code /} on the existing path is dropped before joining, so a context such as {@code /} or
   * {@code /app/} does not produce a doubled separator; the supplied segment should begin with {@code /}.
   *
   * @param path request path segment (should begin with {@code /})
   * @return new target bound to the combined path
   * @throws URISyntaxException if the resulting URI is invalid
   */
  public JsonTarget path (String path)
    throws URISyntaxException {

    if ((this.path == null) || this.path.isBlank()) {

      return new JsonTarget(httpHost, path);
    }

    return new JsonTarget(httpHost, (this.path.endsWith("/") ? this.path.substring(0, this.path.length() - 1) : this.path) + path);
  }

  /**
   * Appends an HTTP header that will be sent with each subsequent request.
   *
   * @param key   header name
   * @param value header value
   * @return this instance for chaining
   */
  public JsonTarget header (String key, String value) {

    if (headers == null) {
      headers = new Tuple<>();
    }
    headers.addPair(key, value);

    return this;
  }

  /**
   * Appends a query parameter to the request URL.
   *
   * @param key   parameter name
   * @param value parameter value
   * @return this instance for chaining
   */
  public JsonTarget query (String key, String value) {

    if (queryParameters == null) {
      queryParameters = new Tuple<>();
    }
    queryParameters.addPair(key, value);

    return this;
  }

  /**
   * Sets the log level used for request and response debug output.
   *
   * @param level desired log level
   * @return this instance for chaining
   */
  public JsonTarget debug (Level level) {

    this.level = level;

    return this;
  }

  /**
   * Executes a GET request and deserializes the response body to the given type.
   *
   * @param responseClass expected return type
   * @param <T>           response type
   * @return deserialized response, or {@code null} if the response body is empty
   * @throws Exception if the request fails or the response cannot be deserialized
   */
  public <T> T get (Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.GET, null);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return readEntity(callback.getResponse(), responseClass);
  }

  /**
   * Executes a PUT request with the supplied JSON body and deserializes the response.
   *
   * @param jsonBody      request body to send
   * @param responseClass expected return type
   * @param <T>           response type
   * @return deserialized response, or {@code null} if the response body is empty
   * @throws Exception if the request fails or the response cannot be deserialized
   */
  public <T> T put (JsonBody jsonBody, Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.PUT, jsonBody);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return readEntity(callback.getResponse(), responseClass);
  }

  /**
   * Executes a POST request with the supplied JSON body and deserializes the response.
   *
   * @param jsonBody      request body to send
   * @param responseClass expected return type
   * @param <T>           response type
   * @return deserialized response, or {@code null} if the response body is empty
   * @throws Exception if the request fails or the response cannot be deserialized
   */
  public <T> T post (JsonBody jsonBody, Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.POST, jsonBody);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return readEntity(callback.getResponse(), responseClass);
  }

  /**
   * Executes a PATCH request with the supplied JSON body and deserializes the response.
   *
   * @param jsonBody      request body to send
   * @param responseClass expected return type
   * @param <T>           response type
   * @return deserialized response, or {@code null} if the response body is empty
   * @throws Exception if the request fails or the response cannot be deserialized
   */
  public <T> T patch (JsonBody jsonBody, Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.PATCH, jsonBody);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return readEntity(callback.getResponse(), responseClass);
  }

  /**
   * Executes a DELETE request and deserializes the response body to the given type.
   *
   * @param responseClass expected return type
   * @param <T>           response type
   * @return deserialized response, or {@code null} if the response body is empty
   * @throws Exception if the request fails or the response cannot be deserialized
   */
  public <T> T delete (Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.DELETE, null);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return readEntity(callback.getResponse(), responseClass);
  }

  private <T> T readEntity (SimpleHttpResponse response, Class<T> responseClass)
    throws IOException {

    SimpleBody body;
    byte[] bodyContent;

    if (((body = response.getBody()) == null) || ((bodyContent = body.getBodyBytes()) == null) || (bodyContent.length == 0)) {
      if ((response.getCode() >= 200) && (response.getCode() < 300)) {

        return null;
      }

      throw new WebApplicationException(response.getReasonPhrase(), response.getCode());
    }

    LoggerManager.getLogger(JsonTarget.class).log(level, new ResponseDebugCollector(response));

    return JsonCodec.read(bodyContent, responseClass);
  }

  private SimpleHttpRequest createHttpRequest (HttpMethod httpMethod, JsonBody jsonBody) {

    SimpleHttpRequest httpRequest;
    String completedPath;

    if (queryParameters == null) {
      completedPath = path;
    } else {

      StringBuilder queryBuilder = new StringBuilder("?");
      boolean first = true;

      for (Pair<String, String> queryPair : queryParameters) {
        if (!first) {
          queryBuilder.append('&');
        }
        queryBuilder.append(queryPair.first()).append('=').append(queryPair.second());
        first = false;
      }

      completedPath = path + queryBuilder;
    }

    httpRequest = switch (httpMethod) {
      case GET -> SimpleHttpRequest.create(Method.GET, httpHost, completedPath);
      case PUT -> SimpleHttpRequest.create(Method.PUT, httpHost, completedPath);
      case POST -> SimpleHttpRequest.create(Method.POST, httpHost, completedPath);
      case PATCH -> SimpleHttpRequest.create(Method.PATCH, httpHost, completedPath);
      case DELETE -> SimpleHttpRequest.create(Method.DELETE, httpHost, completedPath);
      default -> throw new UnknownSwitchCaseException(httpMethod.name());
    };

    if (headers != null) {
      for (Pair<String, String> headerPair : headers) {
        httpRequest.addHeader(headerPair.first(), headerPair.second());
      }
    }

    if (jsonBody != null) {
      httpRequest.setBody(jsonBody.getBodyAsBytes(), jsonBody.getContentType());
    }

    return httpRequest;
  }

  private record ResponseDebugCollector(SimpleHttpResponse response) {

    @Override
    public String toString () {

      StringBuilder debugBuilder = new StringBuilder();

      debugBuilder.append("Receiving client response\n");
      debugBuilder.append("< ").append(response.getCode()).append('\n');
      for (Header header : response.getHeaders()) {
        debugBuilder.append("< ").append(header.getName()).append(": ").append(header.getValue()).append('\n');
      }

      debugBuilder.append(response.getBodyText()).append('\n');

      return debugBuilder.toString();
    }
  }

  private record RequestDebugCollector(SimpleHttpRequest httpRequest) {

    @Override
    public String toString () {

      StringBuilder debugBuilder = new StringBuilder();

      debugBuilder.append("Sending client request\n");
      debugBuilder.append("< ").append(httpRequest.getRequestUri()).append('\n');
      for (Header header : httpRequest.getHeaders()) {
        debugBuilder.append("< ").append(header.getName()).append(": ").append(header.getValue()).append('\n');
      }

      if (httpRequest.getBody() != null) {
        debugBuilder.append(httpRequest.getBody().getBodyText()).append('\n');
      }

      return debugBuilder.toString();
    }
  }
}
