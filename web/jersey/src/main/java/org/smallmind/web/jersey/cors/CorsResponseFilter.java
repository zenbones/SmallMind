package org.smallmind.web.jersey.cors;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

public class CorsResponseFilter implements ContainerResponseFilter {

  @Override
  public void filter (ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

    String accessControlRequestHeaders;

    if (((accessControlRequestHeaders = requestContext.getHeaderString("Access-Control-Request-Headers")) != null) && (!accessControlRequestHeaders.isEmpty())) {
      responseContext.getHeaders().add("Access-Control-Allow-Headers", accessControlRequestHeaders);
    }

    responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
    responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS, HEAD");
    responseContext.getHeaders().add("Access-Control-Allow-Credentials", "false");
  }
}

