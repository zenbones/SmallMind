package org.smallmind.file.ephemeral;

import java.nio.file.attribute.FileStoreAttributeView;

public class EphemeralFileStoreAttributeView implements FileStoreAttributeView {

  @Override
  public String name () {

    return "ephemeral";
  }
}
