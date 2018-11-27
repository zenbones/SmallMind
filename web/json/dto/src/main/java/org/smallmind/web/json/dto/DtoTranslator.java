package org.smallmind.web.json.dto;

import java.io.BufferedWriter;
import java.io.IOException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

public interface DtoTranslator {

  void writeRightSideOfEquals (BufferedWriter writer, String entityInstanceName, String entityFieldName, TypeMirror entityFieldTypeMirror, String dtoFullyQualifiedTypeName)
    throws IOException;

  void writeInsideOfSet (BufferedWriter writer, ProcessingEnvironment processingEnvironment, TypeMirror entityFieldTypeMirror, String dtoFieldName)
    throws IOException;
}
