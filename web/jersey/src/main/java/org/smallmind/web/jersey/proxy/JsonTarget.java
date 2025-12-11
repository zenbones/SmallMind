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

public class JsonTarget {

  private final HttpHost httpHost;
  private Tuple<String, String> headers;
  private Tuple<String, String> queryParameters;
  private Level level = Level.OFF;
  private String path;

  public JsonTarget (String host)
    throws URISyntaxException {

    httpHost = HttpHost.create(host);
  }

  private JsonTarget (HttpHost httpHost, String path) {

    this.httpHost = httpHost;
    this.path = path;
  }

  public JsonTarget path (String path)
    throws URISyntaxException {

    return new JsonTarget(httpHost, path);
  }

  public JsonTarget header (String key, String value) {

    if (headers == null) {
      headers = new Tuple<>();
    }
    headers.addPair(key, value);

    return this;
  }

  public JsonTarget query (String key, String value) {

    if (queryParameters == null) {
      queryParameters = new Tuple<>();
    }
    queryParameters.addPair(key, value);

    return this;
  }

  public JsonTarget debug (Level level) {

    this.level = level;

    return this;
  }

  public <T> T get (Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.GET, null);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return convertEntity(callback.getResponse(), responseClass);
  }

  public <T> T put (JsonBody jsonBody, Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.PUT, jsonBody);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return convertEntity(callback.getResponse(), responseClass);
  }

  public <T> T post (JsonBody jsonBody, Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.POST, jsonBody);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return convertEntity(callback.getResponse(), responseClass);
  }

  public <T> T patch (JsonBody jsonBody, Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.PATCH, jsonBody);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return convertEntity(callback.getResponse(), responseClass);
  }

  public <T> T delete (Class<T> responseClass)
    throws Exception {

    SimpleHttpRequest httpRequest = createHttpRequest(HttpMethod.DELETE, null);
    SimpleCallback callback;

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpRequest));

    HttpClient.execute(httpRequest, callback = new SimpleCallback(), 10);

    return convertEntity(callback.getResponse(), responseClass);
  }

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

  private SimpleHttpRequest createHttpRequest (HttpMethod httpMethod, JsonBody jsonBody) {

    SimpleHttpRequest httpRequest;

    if (queryParameters != null) {

      StringBuilder queryBuilder = new StringBuilder("?");
      boolean first = true;

      for (Pair<String, String> queryPair : queryParameters) {
        if (!first) {
          queryBuilder.append('&');
        }
        queryBuilder.append(queryPair.getFirst()).append('=').append(queryPair.getSecond());
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
        httpRequest.addHeader(headerPair.getFirst(), headerPair.getSecond());
      }
    }

    if (jsonBody != null) {
      httpRequest.setBody(jsonBody.getBodyAsBytes(), jsonBody.getContentType());
    }

    return httpRequest;
  }

  private static class ResponseDebugCollector {

    private final SimpleHttpResponse response;

    private ResponseDebugCollector (SimpleHttpResponse response) {

      this.response = response;
    }

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

  private static class RequestDebugCollector {

    private final SimpleHttpRequest httpRequest;

    private RequestDebugCollector (SimpleHttpRequest httpRequest) {

      this.httpRequest = httpRequest;
    }

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
