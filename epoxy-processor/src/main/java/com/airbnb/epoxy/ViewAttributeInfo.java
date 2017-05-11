package com.airbnb.epoxy;

import android.support.annotation.Nullable;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.AnnotationSpec.Builder;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import java.util.Map.Entry;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import static com.airbnb.epoxy.ProcessorUtils.capitalizeFirstLetter;

class ViewAttributeInfo extends AttributeInfo {
  boolean optional;
  final String originalPropName;
  boolean isOverload;

  ViewAttributeInfo(ModelViewInfo modelInfo, String name, VariableElement paramElement,
      Types types) {
    this(modelInfo, name, paramElement.asType());

    useInHash = true; // TODO: (eli_hart 4/26/17) We should come up with a way to exclude things
    // from the hash (like click listeners). One option is to exclude it if it the type doesn't
    // implement hashCode

    parseAnnotations(paramElement, types);
    optional = paramElement.getAnnotation(Nullable.class) != null;
  }

  ViewAttributeInfo(ModelViewInfo modelInfo, String name, TypeMirror type) {
    typeName = TypeName.get(type);
    typeMirror = type;
    originalPropName = name;

    // Suffix the field name with the type to prevent collisions from overloaded setter methods
    this.name = name + "_" + getSimpleName(typeName);
    modelName = modelInfo.getGeneratedName().simpleName();
    modelPackageName = modelInfo.generatedClassName.packageName();
    ignoreRequireHashCode = false;
    generateSetter = true;
    generateGetter = true;
    hasFinalModifier = false;
    packagePrivate = false;
  }

  /** Tries to return the simple name of the given type. */
  private static String getSimpleName(TypeName name) {
    if (name.isPrimitive()) {
      return capitalizeFirstLetter(name.toString());
    }

    if (name instanceof ClassName) {
      return ((ClassName) name).simpleName();
    }

    if (name instanceof ArrayTypeName) {
      return getSimpleName(((ArrayTypeName) name).componentType) + "Array";
    }

    if (name instanceof ParameterizedTypeName) {
      return getSimpleName(((ParameterizedTypeName) name).rawType);
    }

    if (name instanceof TypeVariableName) {
      return capitalizeFirstLetter(((TypeVariableName) name).name);
    }

    // Don't expect this to happen
    return name.toString().replace(".", "");
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

  @Override
  String generatedSetterName() {
    return originalPropName;
  }

  @Override
  String generatedGetterName() {
    if (isOverload) {
      // Avoid method name collisions for overloaded method by appending the return type
      return originalPropName + getSimpleName(typeName);
    }

    return originalPropName;
  }
}
