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
  private static final Pattern PATTERN_STARTS_WITH_SET = Pattern.compile("set[A-Z]\\w*");
  private final TypeElement viewElement;
  private final Types typeUtils;

  ModelViewInfo(TypeElement viewElement, Types typeUtils, Elements elementUtils) {
    this.viewElement = viewElement;
    this.typeUtils = typeUtils;
    superClassElement =
        (TypeElement) ProcessorUtils.getElementByName(ClassNames.EPOXY_MODEL_UNTYPED,
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
    className += "Model" + GENERATED_CLASS_NAME_SUFFIX;

    return ClassName.get(packageName, className);
  }

  void addProp(ExecutableElement propMethod) {
    VariableElement param = propMethod.getParameters().get(0);

    String propName = propMethod.getSimpleName().toString();
    if (PATTERN_STARTS_WITH_SET.matcher(propName).matches()) {
      propName = toLowerCase(propName.charAt(3)) + propName.substring(4);
    }

    // TODO: (eli_hart 4/28/17) optional/nullable support along with default value

    // TODO: (eli_hart 4/28/17) check for other setters of the same name
    addAttribute(new ViewAttributeInfo(this, propName, param, typeUtils));
  }

  void addResetMethod(ExecutableElement resetMethod) {
    resetMethodNames.add(resetMethod.getSimpleName().toString());
  }

  LayoutResource getLayoutResource(LayoutResourceProcessor layoutResourceProcessor) {
    return layoutResourceProcessor.getLayoutInAnnotation(viewElement, ModelView.class);
  }

  List<String> getResetMethodNames() {
    return resetMethodNames;
  }
}
