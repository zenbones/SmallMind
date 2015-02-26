package org.smallmind.artifact.maven;

import java.util.EventListener;

public interface MavenScannerListener extends EventListener {

  public void artifactChange (MavenScannerEvent event);
}
