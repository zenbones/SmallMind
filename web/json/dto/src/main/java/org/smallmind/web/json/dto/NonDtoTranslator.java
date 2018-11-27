package org.smallmind.web.json.dto;

import java.io.BufferedWriter;
import java.io.IOException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;

public class NonDtoTranslator implements DtoTranslator {

  @Override
  public void writeRightSideOfEquals (BufferedWriter writer, String entityInstanceName, String entityFieldName, TypeMirror entityFieldTypeMirror, String dtoFullyQualifiedTypeName)
    throws IOException {

    writer.write(entityInstanceName);
    writer.write(".");
    writer.write(TypeKind.BOOLEAN.equals(entityFieldTypeMirror.getKind()) ? BeanUtility.asIsName(entityFieldName) : BeanUtility.asGetterName(entityFieldName));
    writer.write("()");
    writer.write(";");
  }

  @Override
  public void writeInsideOfSet (BufferedWriter writer, ProcessingEnvironment processingEnvironment, TypeMirror entityFieldTypeMirror, String dtoFieldName)
    throws IOException {

    writer.write(dtoFieldName);
  }
}
