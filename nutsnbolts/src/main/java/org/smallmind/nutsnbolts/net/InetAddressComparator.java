package org.smallmind.nutsnbolts.net;

import java.net.InetAddress;
import java.util.Comparator;

public class InetAddressComparator implements Comparator<InetAddress> {

  @Override
  public int compare (InetAddress a, InetAddress b) {

    byte[] aOctets = a.getAddress();
    byte[] bOctets = b.getAddress();
    int len = Math.max(aOctets.length, bOctets.length);

    for (int i = 0; i < len; i++) {
      byte aOctet = (i >= len - aOctets.length) ? aOctets[i - (len - aOctets.length)] : 0;
      byte bOctet = (i >= len - bOctets.length) ? bOctets[i - (len - bOctets.length)] : 0;

      if (aOctet != bOctet) return (0xff & aOctet) - (0xff & bOctet);
    }

    return 0;
  }
}
