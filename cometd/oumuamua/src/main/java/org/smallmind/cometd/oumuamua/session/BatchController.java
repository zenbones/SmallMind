package org.smallmind.cometd.oumuamua.session;

import java.util.LinkedList;
import org.smallmind.cometd.oumuamua.OumuamuaServerSession;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;

public class BatchController {

  private LinkedList<OumuamuaPacket> packetList;
  private int count = 0;

  public boolean isActive () {

    return count > 0;
  }

  public void start () {

    if (count++ == 0) {
      packetList = new LinkedList<>();
    }
  }

  public void addPacket (OumuamuaPacket packet) {

    if (count <= 0) {
      throw new IllegalStateException("No current batch");
    } else {
      packetList.add(packet);
    }
  }

  public boolean end (OumuamuaServerSession serverSession) {

    if (--count > 0) {

      return false;
    } else {
      for (OumuamuaPacket batchedPacket : packetList) {
        serverSession.send(batchedPacket);
      }

      return true;
    }
  }
}
