package org.smallmind.change.git;

import java.io.InputStream;
import java.net.URL;
import org.smallmind.nutsnbolts.lang.ClassGate;
import org.smallmind.nutsnbolts.lang.ClassStreamTicket;

public class GitClassGate implements ClassGate {

  @Override
  public ClassStreamTicket getClassAsTicket (String name) {

    return null;
  }

  @Override
  public URL getResource (String path) {

    return null;
  }

  @Override
  public InputStream getResourceAsStream (String path) {

    return null;
  }

  @Override
  public long getLastModDate (String path) {

    return 0;
  }
}
