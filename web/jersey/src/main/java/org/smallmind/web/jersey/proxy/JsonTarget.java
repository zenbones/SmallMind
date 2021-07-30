/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.WebApplicationException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.smallmind.nutsnbolts.http.HttpMethod;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.Pair;
import org.smallmind.nutsnbolts.util.Tuple;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class JsonTarget {

  private final CloseableHttpClient httpClient;
  private final URI uri;
  private Tuple<String, String> headers;
  private Tuple<String, String> queryParameters;
  private Level level = Level.OFF;

  public JsonTarget (CloseableHttpClient httpClient, URI uri) {

    this.httpClient = httpClient;
    this.uri = uri;
  }

  public JsonTarget path (String path)
    throws URISyntaxException {

    URIBuilder uriBuilder = new URIBuilder().setScheme(uri.getScheme()).setUserInfo(uri.getUserInfo()).setHost(uri.getHost()).setPort(uri.getPort()).setPath(uri.getPath()).setCustomQuery(uri.getQuery()).setFragment(uri.getFragment());
    URI pathURI = URI.create(path);

    if (pathURI.getScheme() != null) {
      uriBuilder.setScheme(pathURI.getScheme());
    }
    if (pathURI.getUserInfo() != null) {
      uriBuilder.setUserInfo(pathURI.getUserInfo());
    }
    if (pathURI.getHost() != null) {
      uriBuilder.setHost(pathURI.getHost());
    }
    if (pathURI.getPort() > 0) {
      uriBuilder.setPort(pathURI.getPort());
    }
    if (pathURI.getPath() != null) {
      uriBuilder.setPath((uri.getPath() == null) ? pathURI.getPath() : uri.getPath() + pathURI.getPath());
    }
    if (pathURI.getRawQuery() != null) {
      uriBuilder.setCustomQuery((uri.getRawQuery() == null) ? pathURI.getQuery() : uri.getQuery() + "&" + pathURI.getQuery());
    }
    if (pathURI.getFragment() != null) {
      uriBuilder.setFragment(pathURI.getFragment());
    }

    return new JsonTarget(httpClient, uriBuilder.build());
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
    throws IOException, URISyntaxException {

    HttpGet httpGet = ((HttpGet)createHttpRequest(HttpMethod.GET, null));

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpGet, null));

    try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
      return convertEntity(response, responseClass);
    }
  }

  public <T> T put (HttpEntity entity, Class<T> responseClass)
    throws IOException, URISyntaxException {

    HttpPut httpPut = ((HttpPut)createHttpRequest(HttpMethod.PUT, entity));

    httpPut.setEntity(entity);
    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpPut, entity));

    try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
      return convertEntity(response, responseClass);
    }
  }

  public <T> T post (HttpEntity entity, Class<T> responseClass)
    throws IOException, URISyntaxException {

    HttpPost httpPost = ((HttpPost)createHttpRequest(HttpMethod.POST, entity));

    httpPost.setEntity(entity);
    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpPost, entity));

    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
      return convertEntity(response, responseClass);
    }
  }

  public <T> T patch (HttpEntity entity, Class<T> responseClass)
    throws IOException, URISyntaxException {

    HttpPatch httpPatch = ((HttpPatch)createHttpRequest(HttpMethod.PATCH, entity));

    httpPatch.setEntity(entity);
    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpPatch, entity));

    try (CloseableHttpResponse response = httpClient.execute(httpPatch)) {
      return convertEntity(response, responseClass);
    }
  }

  public <T> T delete (Class<T> responseClass)
    throws IOException, URISyntaxException {

    HttpDelete httpDelete = ((HttpDelete)createHttpRequest(HttpMethod.DELETE, null));

    LoggerManager.getLogger(JsonTarget.class).log(level, new RequestDebugCollector(httpDelete, null));

    try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
      return convertEntity(response, responseClass);
    }
  }

  private <T> T convertEntity (HttpResponse response, Class<T> responseClass)
    throws IOException {

    HttpEntity entity;
    InputStreamHolder entityInputStreamHolder;

    if (((entity = response.getEntity()) == null) || (entity.getContentLength() == 0) || (entityInputStreamHolder = new InputStreamHolder(entity.getContent())).isEmpty()) {

      StatusLine statusLine;

      if (((statusLine = response.getStatusLine()) == null) || ((statusLine.getStatusCode() >= 200) && statusLine.getStatusCode() < 300)) {

        return null;
      }

      throw new WebApplicationException(statusLine.getReasonPhrase(), statusLine.getStatusCode());
    }

    LoggerManager.getLogger(JsonTarget.class).log(level, new ResponseDebugCollector(response, entityInputStreamHolder));

    return JsonCodec.read(entityInputStreamHolder.getInputStream(), responseClass);
  }

  private HttpRequest createHttpRequest (HttpMethod httpMethod, HttpEntity entity)
    throws URISyntaxException {

    HttpRequest httpRequest;

    if (queryParameters == null) {
      switch (httpMethod) {
        case GET:
          httpRequest = new HttpGet(uri);
          break;
        case PUT:
          httpRequest = new HttpPut(uri);
          break;
        case POST:
          httpRequest = new HttpPost(uri);
          break;
        case PATCH:
          httpRequest = new HttpPatch(uri);
          break;
        case DELETE:
          httpRequest = new HttpDelete(uri);
          break;
        default:
          throw new UnknownSwitchCaseException(httpMethod.name());
      }
    } else {

      URIBuilder uriBuilder = new URIBuilder(uri);

      for (Pair<String, String> queryPair : queryParameters) {
        uriBuilder.addParameter(queryPair.getFirst(), queryPair.getSecond());
      }

      switch (httpMethod) {
        case GET:
          httpRequest = new HttpGet(uriBuilder.build());
          break;
        case PUT:
          httpRequest = new HttpPut(uriBuilder.build());
          break;
        case POST:
          httpRequest = new HttpPost(uriBuilder.build());
          break;
        case PATCH:
          httpRequest = new HttpPatch(uriBuilder.build());
          break;
        case DELETE:
          httpRequest = new HttpDelete(uriBuilder.build());
          break;
        default:
          throw new UnknownSwitchCaseException(httpMethod.name());
      }
    }

    if (headers != null) {
      for (Pair<String, String> headerPair : headers) {
        httpRequest.addHeader(headerPair.getFirst(), headerPair.getSecond());
      }
    }

    if (entity != null) {

      Header contentTypeHeader;

      if ((contentTypeHeader = entity.getContentType()) != null) {
        httpRequest.setHeader(contentTypeHeader.getName(), contentTypeHeader.getValue());
      }
    }

    return httpRequest;
  }

  private class InputStreamHolder {

    private InputStream inputStream;

    private InputStreamHolder (InputStream inputStream) {

      this.inputStream = inputStream;
    }

    private InputStream getInputStream () {

      return inputStream;
    }

    private void setInputStream (InputStream inputStream) {

      this.inputStream = inputStream;
    }

    public boolean isEmpty () {

      return inputStream == null;
    }
  }

  private class RequestDebugCollector {

    private final HttpRequest httpRequest;
    private final HttpEntity entity;

    private RequestDebugCollector (HttpRequest httpRequest, HttpEntity entity) {

      this.httpRequest = httpRequest;
      this.entity = entity;
    }

    @Override
    public String toString () {

      StringBuilder debugBuilder = new StringBuilder();

      debugBuilder.append("Sending client request\n");
      debugBuilder.append("< ").append(httpRequest.getRequestLine()).append('\n');
      for (Header header : httpRequest.getAllHeaders()) {
        debugBuilder.append("< ").append(header.getName()).append(": ").append(header.getValue()).append('\n');
      }

      if (entity != null) {
        try {
          debugBuilder.append(EntityUtils.toString(entity)).append('\n');
        } catch (IOException ioException) {
          throw new RuntimeException(ioException);
        }
      }

      return debugBuilder.toString();
    }
  }

  private class ResponseDebugCollector {

    private final HttpResponse response;
    private final InputStreamHolder entityInputStreamHolder;

    private ResponseDebugCollector (HttpResponse response, InputStreamHolder entityInputStreamHolder) {

      this.response = response;
      this.entityInputStreamHolder = entityInputStreamHolder;
    }

    @Override
    public String toString () {

      StringBuilder debugBuilder = new StringBuilder();
      InputStream entityInputStream = entityInputStreamHolder.getInputStream();
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      int singleByte;

      debugBuilder.append("Receiving client response\n");
      debugBuilder.append("< ").append(response.getStatusLine().getStatusCode()).append('\n');
      for (Header header : response.getAllHeaders()) {
        debugBuilder.append("< ").append(header.getName()).append(": ").append(header.getValue()).append('\n');
      }

      try {
        while ((singleByte = entityInputStream.read()) >= 0) {
          byteArrayOutputStream.write(singleByte);
        }

        byteArrayOutputStream.close();
      } catch (IOException ioException) {
        throw new RuntimeException(ioException);
      }

      entityInputStreamHolder.setInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
      debugBuilder.append(byteArrayOutputStream).append('\n');

      return debugBuilder.toString();
    }
  }
}
