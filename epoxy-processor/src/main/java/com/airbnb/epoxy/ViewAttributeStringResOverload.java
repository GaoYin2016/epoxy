package com.airbnb.epoxy;

import android.support.annotation.StringRes;

import com.squareup.javapoet.AnnotationSpec;

import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;

class ViewAttributeStringResOverload extends ViewAttributeInfo {

  ViewAttributeStringResOverload(ModelViewInfo viewInfo, ViewAttributeInfo info, Types types) {
    super(viewInfo, info.originalPropName, types.getPrimitiveType(TypeKind.INT));
    useInHash = true; // TODO: (eli_hart 4/26/17) We should come up with a way to exclude things
    // from the hash (like click listeners). One option is to exclude it if it the type doesn't
    // implement hashCode

    optional = info.optional;
    isOverload = true;

    AnnotationSpec stringResAnnotation = AnnotationSpec.builder(StringRes.class).build();
    getterAnnotations.add(stringResAnnotation);
    setterAnnotations.add(stringResAnnotation);
  }
}
