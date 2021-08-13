package org.smallmind.file.jailed;

import java.io.IOException;
import java.nio.file.Path;

public interface JailedPathTranslator {

  Path wrapPath (JailedFileSystem jailedFileSystem, Path nativePath)
    throws IOException;

  Path unwrapPath (Path jailedPath)
    throws IOException;
}
