package com.airbnb.epoxy;

import android.support.annotation.Nullable;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.AnnotationSpec.Builder;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.util.Map.Entry;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;

import static com.airbnb.epoxy.ProcessorUtils.capitalizeFirstLetter;

class ViewAttributeInfo extends AttributeInfo {
  boolean optional;
  String viewSetterName;

  ViewAttributeInfo(ModelViewInfo modelInfo, String name, VariableElement paramElement,
      Types types) {
    typeName = TypeName.get(paramElement.asType());
    typeMirror = paramElement.asType();
    viewSetterName = name;
    // Include the type in the field name so that overloaded setters will work
    this.name = name + capitalizeFirstLetter(typeName.toString());
    modelName = modelInfo.getGeneratedName().simpleName();
    modelPackageName = modelInfo.generatedClassName.packageName();
    useInHash = true; // TODO: (eli_hart 4/26/17) We should come up with a way to exclude things
    // from the hash (like click listeners). One option is to exclude it if it the type doesn't
    // implement hashCode
    ignoreRequireHashCode = false;
    generateSetter = true;
    generateGetter = true;
    hasFinalModifier = false;
    packagePrivate = false;

    parseAnnotations(paramElement, types);
    optional = paramElement.getAnnotation(Nullable.class) != null;
  }

  ViewAttributeInfo() {
  }

  private void parseAnnotations(VariableElement paramElement, Types types) {
    for (AnnotationMirror annotationMirror : paramElement.getAnnotationMirrors()) {
      Element annotationElement = types.asElement(annotationMirror.getAnnotationType());
      Builder builder = AnnotationSpec.builder(ClassName.get(((TypeElement) annotationElement)));

      for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror
          .getElementValues().entrySet()) {
        String paramName = entry.getKey().getSimpleName().toString();
        String paramValue = entry.getValue().getValue().toString();
        builder.addMember(paramName, paramValue);
      }

      AnnotationSpec annotationSpec = builder.build();
      setterAnnotations.add(annotationSpec);
      getterAnnotations.add(annotationSpec);
    }
  }
}
