package com.airbnb.epoxy;

import com.squareup.javapoet.TypeName;

import javax.lang.model.element.VariableElement;

class ViewAttributeInfo extends AttributeInfo {

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
