package com.airbnb.epoxy;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static java.lang.Character.toLowerCase;

class ModelViewInfo extends GeneratedModelInfo {
  private final List<String> resetMethodNames = new ArrayList<>();
  private static final Pattern PATTERN_STARTS_WITH_SET = Pattern.compile("set[w]+");

  ModelViewInfo(TypeElement viewElement, Types typeUtils, Elements elementUtils) {
    superClassElement =
        (TypeElement) ProcessorUtils.getElementByName(ProcessorUtils.EPOXY_MODEL_TYPE,
            elementUtils, typeUtils);

    this.superClassName = ParameterizedTypeName
        .get(ClassNames.EPOXY_MODEL_UNTYPED, TypeName.get(viewElement.asType()));

    generatedClassName = buildGeneratedModelName(viewElement, elementUtils);
    // We don't have any type parameters on our generated model
    this.parametrizedClassName = generatedClassName;
    shouldGenerateModel = true;
    generateFieldsForAttributes = true;

    collectMethodsReturningClassType(superClassElement, typeUtils);

    // The bound type is the type of this view
    boundObjectTypeName = ClassName.get(viewElement.asType());
  }

  private ClassName buildGeneratedModelName(TypeElement viewElement, Elements elementUtils) {
    String packageName = elementUtils.getPackageOf(viewElement).getQualifiedName().toString();

    String className = viewElement.getSimpleName().toString();
    if (className.endsWith("View")) {
      className = className.substring(0, className.lastIndexOf("View"));
    }

    className += "Model" + GENERATED_CLASS_NAME_SUFFIX;

    return ClassName.get(packageName, className);
  }

  void addProp(ExecutableElement propMethod) {
    List<? extends VariableElement> parameters = propMethod.getParameters();
    VariableElement param = parameters.get(0);

    String methodName = propMethod.getSimpleName().toString();
    if (PATTERN_STARTS_WITH_SET.matcher(methodName).matches()) {
      methodName = methodName.replace("set", "");
      methodName = toLowerCase(methodName.charAt(0)) + methodName.substring(1);
    }

      // TODO: (eli_hart 4/28/17) optional/nullable support along with default value

    // TODO: (eli_hart 4/28/17) check for other setters of the same name
    addAttribute(new ViewAttributeInfo(this, methodName, param));
  }

  static class ViewAttributeInfo extends AttributeInfo {

    ViewAttributeInfo(ModelViewInfo modelInfo, String name, VariableElement paramElement) {
      this.name = name;
      typeName = TypeName.get(paramElement.asType());
      typeMirror = paramElement.asType();
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
    }
  }

  void addResetMethod(ExecutableElement resetMethod) {
    resetMethodNames.add(resetMethod.getSimpleName().toString());
  }
}
