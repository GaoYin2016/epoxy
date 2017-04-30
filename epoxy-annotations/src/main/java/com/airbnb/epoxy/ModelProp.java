package com.airbnb.epoxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ModelProp {
  int intDefault() default 0;
  float floatDefault() default 0;
  double doubleDefault() default 0;
  boolean booleanDefault() default false;
}
