package org.smallmind.web.jersey.reflection;

import java.io.IOException;
import java.util.LinkedList;
import org.smallmind.web.jersey.util.JsonCodec;

public class PathComponent {

  private LinkedList<Integer> subscriptList;
  private Object[] arguments;
  private String name;

  public PathComponent (String name) {

    this.name = name;
  }

  public String getName () {

    return name;
  }

  public Object[] getArguments () {

    return arguments;
  }

  public void createArguments (String argumentSubPath)
    throws IOException {

    arguments = JsonCodec.read('[' + argumentSubPath.trim() + ']', Object[].class);
  }

  public int[] getSubscripts () {

    if ((subscriptList == null) || subscriptList.isEmpty()) {

      return null;
    } else {

      int[] subscripts = new int[subscriptList.size()];
      int index = 0;

      for (Integer subscript : subscriptList) {
        subscripts[index++] = subscript;
      }

      return subscripts;
    }
  }

  public void addSubscript (String subscriptSubPath) {

    if (subscriptList == null) {
      subscriptList = new LinkedList<>();
    }
    subscriptList.add(Integer.parseInt(subscriptSubPath.trim()));
  }
}
