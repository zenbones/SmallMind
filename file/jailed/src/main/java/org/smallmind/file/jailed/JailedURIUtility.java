package org.smallmind.file.jailed;

import java.net.URI;
import java.nio.file.Path;

public class JailedURIUtility {

  public static void checkUri (String scheme, URI uri) {

    if (!uri.getScheme().equalsIgnoreCase(scheme)) {
      throw new IllegalArgumentException("URI does not match this provider");
    } else if (uri.getAuthority() != null) {
      throw new IllegalArgumentException("URI has an authority component");
    } else if (uri.getPath() == null) {
      throw new IllegalArgumentException("Path component is undefined");
    } else if (!uri.getPath().equals("/")) {
      throw new IllegalArgumentException("Path component should be '/'");
    } else if (uri.getQuery() != null) {
      throw new IllegalArgumentException("URI has a query component");
    } else if (uri.getFragment() != null) {
      throw new IllegalArgumentException("URI has a fragment component");
    }
  }

  public static Path fromUri (JailedFileSystem jailedFileSystem, URI uri) {

    if (!uri.isAbsolute()) {
      throw new IllegalArgumentException("URI is not absolute");
    } else if (uri.isOpaque()) {
      throw new IllegalArgumentException("URI is not hierarchical");
    } else {

      String scheme = uri.getScheme();

      if ((scheme == null) || !scheme.equalsIgnoreCase(jailedFileSystem.provider().getScheme())) {
        throw new IllegalArgumentException("URI does not match this provider");
      } else if (uri.getAuthority() != null) {
        throw new IllegalArgumentException("URI has an authority component");
      } else if (uri.getFragment() != null) {
        throw new IllegalArgumentException("URI has a fragment component");
      } else if (uri.getQuery() != null) {
        throw new IllegalArgumentException("URI has a query component");
      }

      return new JailedPath(jailedFileSystem, uri.getPath());
    }
  }
}
