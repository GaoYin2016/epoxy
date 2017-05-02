package com.airbnb.epoxy;

import android.support.annotation.StringRes;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.util.Elements;

import static com.airbnb.epoxy.ProcessorUtils.capitalizeFirstLetter;

class ViewAttributeStringResOverload extends ViewAttributeInfo {

  ViewAttributeStringResOverload(ViewAttributeInfo info, Elements elementUtils) {
    typeName = TypeName.INT;
    typeMirror = ProcessorUtils.getTypeMirror(int.class, elementUtils);

    viewSetterName = name;
    // Include the type in the field name so that overloaded setters will work
    this.name = name + capitalizeFirstLetter(typeName.toString());
    modelName = info.modelName;
    modelPackageName = info.modelPackageName;
    useInHash = true; // TODO: (eli_hart 4/26/17) We should come up with a way to exclude things
    // from the hash (like click listeners). One option is to exclude it if it the type doesn't
    // implement hashCode
    ignoreRequireHashCode = false;
    generateSetter = true;
    generateGetter = true;
    hasFinalModifier = false;
    packagePrivate = false;

    optional = info.optional;

    AnnotationSpec stringResAnnotation = AnnotationSpec.builder(StringRes.class).build();
    getterAnnotations.add(stringResAnnotation);
    setterAnnotations.add(stringResAnnotation);
  }
}
