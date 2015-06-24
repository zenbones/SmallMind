package org.smallmind.phalanx.wire;

public interface WiredService {

  public abstract int getVersion ();

  public abstract String getServiceName ();

  public abstract void setResponseTransport (ResponseTransport responseTransport)
    throws Exception;
}
