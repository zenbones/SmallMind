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
 * Fluent HTTP client wrapper that posts JSON payloads to a configured host and converts responses.
 */
public class JsonTarget {

  private final HttpHost httpHost;
  private Tuple<String, String> headers;
  private Tuple<String, String> queryParameters;
  private Level level = Level.OFF;
  private String path;

  /**
   * Creates a target for the given host URI.
   *
   * @param host URI string including scheme and host
   * @throws URISyntaxException if the host cannot be parsed
   */
  public JsonTarget (String host)
    throws URISyntaxException {

    httpHost = HttpHost.create(host);
  }

  /**
   * Internal constructor used for path derivation.
   *
   * @param httpHost resolved host
   * @param path     request path
   */
  private JsonTarget (HttpHost httpHost, String path) {

    this.httpHost = httpHost;
    this.path = path;
  }

  /**
   * Produces a new JsonTarget with the supplied path.
   *
   * @param path request path
   * @return new target pointing to the derived path
   * @throws URISyntaxException if the combined URI is invalid
   */
  public JsonTarget path (String path)
    throws URISyntaxException {

    return new JsonTarget(httpHost, path);
  }

  /**
   * Adds an HTTP header to subsequent requests.
   *
   * @param key   header name
   * @param value header value
   * @return this for chaining
   */
  public JsonTarget header (String key, String value) {

    if (headers == null) {
      headers = new Tuple<>();
    }
    headers.addPair(key, value);

    return this;
  }

  /**
   * Adds a query parameter to the request URL.
   *
   * @param key   parameter name
   * @param value parameter value
   * @return this for chaining
   */
  public JsonTarget query (String key, String value) {

    if (queryParameters == null) {
      queryParameters = new Tuple<>();
    }
    queryParameters.addPair(key, value);

    return this;
  }

  /**
   * Sets the debug log level for request/response tracing.
   *
   * @param level log level
   * @return this for chaining
   */
  public JsonTarget debug (Level level) {

    this.level = level;

    return this;
  }

  /**
   * Issues a GET request and converts the response body to the requested type.
   *
   * @param responseClass expected response type
   * @return deserialized response or {@code null} when no body is returned
   * @throws Exception if the request fails or conversion fails
   */
  public <T> T get (Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.GET, null);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return convertEntity(callback.getResponse(), responseClass);
  }

  /**
   * Issues a PUT request with a JSON body and converts the response.
   *
   * @param jsonBody      serialized request body
   * @param responseClass expected response type
   * @return deserialized response or {@code null} when no body is returned
   * @throws Exception if the request fails or conversion fails
   */
  public <T> T put (JsonBody jsonBody, Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.PUT, jsonBody);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return convertEntity(callback.getResponse(), responseClass);
  }

  /**
   * Issues a POST request with a JSON body and converts the response.
   *
   * @param jsonBody      serialized request body
   * @param responseClass expected response type
   * @return deserialized response or {@code null} when no body is returned
   * @throws Exception if the request fails or conversion fails
   */
  public <T> T post (JsonBody jsonBody, Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.POST, jsonBody);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return convertEntity(callback.getResponse(), responseClass);
  }

  /**
   * Issues a PATCH request with a JSON body and converts the response.
   *
   * @param jsonBody      serialized request body
   * @param responseClass expected response type
   * @return deserialized response or {@code null} when no body is returned
   * @throws Exception if the request fails or conversion fails
   */
  public <T> T patch (JsonBody jsonBody, Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.PATCH, jsonBody);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return convertEntity(callback.getResponse(), responseClass);
  }

  /**
   * Issues a DELETE request and converts the response.
   *
   * @param responseClass expected response type
   * @return deserialized response or {@code null} when no body is returned
   * @throws Exception if the request fails or conversion fails
   */
  public <T> T delete (Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.DELETE, null);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return convertEntity(callback.getResponse(), responseClass);
  }

  /**
   * Converts a HTTP response into the requested type, throwing {@link WebApplicationException} on error responses.
   *
   * @param response      HTTP response from the client
   * @param responseClass class to deserialize into
   * @return converted entity or {@code null} if the response has no body and is successful
   * @throws WebApplicationException for non-success responses without bodies
   */
  private <T> T convertEntity (SimpleHttpResponse response, Class<T> responseClass) {

    SimpleBody body;
    byte[] bodyContent;

    if (((body = response.getBody()) == null) || ((bodyContent = body.getBodyBytes()) == null) || (bodyContent.length == 0)) {
      if ((response.getCode() >= 200) && (response.getCode() < 300)) {

        return null;
      }

      throw new WebApplicationException(response.getReasonPhrase(), response.getCode());
    }

    LoggerManager.getLogger(JsonTarget.class).log(level, new ResponseDebugCollector(response));

    return JsonCodec.convert(bodyContent, responseClass);
  }

  /**
   * Builds an HTTP request with headers, query parameters, and optional JSON body.
   *
   * @param httpMethod HTTP method to use
   * @param jsonBody   optional JSON body
   * @return configured {@link SimpleHttpRequest}
   * @throws UnknownSwitchCaseException if an unsupported method is provided
   */
  private SimpleHttpRequest createHttpRequest (HttpMethod httpMethod, JsonBody jsonBody) {

    SimpleHttpRequest httpRequest;

    if (queryParameters != null) {

      StringBuilder queryBuilder = new StringBuilder("?");
      boolean first = true;

      for (Pair<String, String> queryPair : queryParameters) {
        if (!first) {
          queryBuilder.append('&');
        }
        queryBuilder.append(queryPair.first()).append('=').append(queryPair.second());
        first = false;
      }

      path = path + queryBuilder;
    }

    switch (httpMethod) {
      case GET:
        httpRequest = SimpleHttpRequest.create(Method.GET, httpHost, path);
        break;
      case PUT:
        httpRequest = SimpleHttpRequest.create(Method.PUT, httpHost, path);
        break;
      case POST:
        httpRequest = SimpleHttpRequest.create(Method.POST, httpHost, path);
        break;
      case PATCH:
        httpRequest = SimpleHttpRequest.create(Method.PATCH, httpHost, path);
        break;
      case DELETE:
        httpRequest = SimpleHttpRequest.create(Method.DELETE, httpHost, path);
        break;
      default:
        throw new UnknownSwitchCaseException(httpMethod.name());
    }

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

  /**
   * Formats response debug output for logging.
   */
  private static class ResponseDebugCollector {

    private final SimpleHttpResponse response;

    /**
     * Creates a collector for the provided response.
     *
     * @param response response to format
     */
    private ResponseDebugCollector (SimpleHttpResponse response) {

      this.response = response;
    }

    /**
     * Returns the response details including status, headers, and body.
     *
     * @return formatted response debug text
     */
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

  /**
   * Formats request debug output for logging.
   */
  private static class RequestDebugCollector {

    private final SimpleHttpRequest httpRequest;

    /**
     * Creates a collector for the provided request.
     *
     * @param httpRequest request to format
     */
    private RequestDebugCollector (SimpleHttpRequest httpRequest) {

      this.httpRequest = httpRequest;
    }

    /**
     * Returns the request URI, headers, and body if present.
     *
     * @return formatted request debug text
     */
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
