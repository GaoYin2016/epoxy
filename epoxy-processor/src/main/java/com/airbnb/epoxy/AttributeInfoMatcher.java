package com.airbnb.epoxy;

import com.squareup.javapoet.TypeName;

/** Used for searching an attribute by equals. */
class AttributeInfoMatcher extends AttributeInfo {

  AttributeInfoMatcher(String name, TypeName typeName) {
    this.name = name;
    this.typeName = typeName;
  }
}
