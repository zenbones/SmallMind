package org.smallmind.file.ephemeral;

public class EphemeralFileSystemConfiguration {

  private final String[] roots;
  private final long capacity;
  private final int blockSize;

  public EphemeralFileSystemConfiguration (long capacity, int blockSize, String... roots) {

    if ((capacity <= 0) || (blockSize <= 0)) {
      throw new IllegalArgumentException("Both capacity and block size must be > 0");
    } else if ((roots == null) || (roots.length == 0)) {
      throw new IllegalArgumentException("At least 1 root path must be specified");
    } else {

      for (String root : roots) {
        if (!root.startsWith(EphemeralPath.getSeparator())) {
          throw new IllegalArgumentException("All roots must start with " + EphemeralPath.getSeparator());
        }
      }

      this.capacity = capacity;
      this.blockSize = blockSize;
      this.roots = roots;
    }
  }

  public String[] getRoots () {

    return roots;
  }

  public long getCapacity () {

    return capacity;
  }

  public int getBlockSize () {

    return blockSize;
  }

  public boolean owned (String first, String... more) {

    for (String root : roots) {

      int position = 0;
      int match = -1;

      for (int index = 0; index < root.length(); index++) {

        String toBeMatched = (match < 0) ? first : more[match];

        if (position == toBeMatched.length()) {
          match ++;
          position = 0;
        }

      }
    }
  }
}
