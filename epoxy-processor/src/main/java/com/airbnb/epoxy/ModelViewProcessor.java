package com.airbnb.epoxy;

import com.airbnb.epoxy.GeneratedModelWriter.BuilderHooks;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static com.airbnb.epoxy.ProcessorUtils.isSubtypeOfType;
import static com.airbnb.epoxy.ProcessorUtils.isType;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

// TODO: (eli_hart 5/1/17) How to create view: layout resource from view annotation?
// TODO: (eli_hart 5/1/17) configuration for default layout resource name
// TODO: (eli_hart 5/1/17) Default model name? Shoumonild we allow customization?
// TODO: (eli_hart 5/1/17) custom base class or interfaces
// TODO: (eli_hart 5/1/17) default value for optional primitives
// TODO: (eli_hart 5/1/17) validate non nullable fields are set
// TODO: (eli_hart 5/3/17) should saved view state
// TODO: (eli_hart 5/3/17) span count
class ModelViewProcessor {
  private final Elements elements;
  private final Types types;
  private final ConfigManager configManager;
  private final ErrorLogger errorLogger;
  private final GeneratedModelWriter modelWriter;
  private final Map<Element, ModelViewInfo> modelClassMap = new LinkedHashMap<>();

  ModelViewProcessor(Elements elements, Types types, ConfigManager configManager,
      ErrorLogger errorLogger, GeneratedModelWriter modelWriter) {

    this.elements = elements;
    this.types = types;
    this.configManager = configManager;
    this.errorLogger = errorLogger;
    this.modelWriter = modelWriter;
  }

  Collection<? extends GeneratedModelInfo> process(RoundEnvironment roundEnv) {
    modelClassMap.clear();

    processViewAnnotations(roundEnv);

    processSetterAnnotations(roundEnv);

    processResetAnnotations(roundEnv);

    writeJava();

    return modelClassMap.values();
  }

  private void processViewAnnotations(RoundEnvironment roundEnv) {
    for (Element viewElement : roundEnv.getElementsAnnotatedWith(ModelView.class)) {
      try {
        if (!validateViewElement(viewElement)) {
          continue;
        }

        modelClassMap.put(viewElement,
            new ModelViewInfo((TypeElement) viewElement, types, elements));
      } catch (Exception e) {
        errorLogger.logError(e, "Error creating model view info classes.");
      }
    }
  }

  private boolean validateViewElement(Element viewElement) {
    if (viewElement.getKind() != ElementKind.CLASS || !(viewElement instanceof TypeElement)) {
      errorLogger.logError("%s annotations can only be on a class (element: %s)", ModelView.class,
          viewElement.getSimpleName());
      return false;
    }

    Set<Modifier> modifiers = viewElement.getModifiers();
    if (modifiers.contains(PRIVATE)) {
      errorLogger.logError(
          "%s annotations must not be on private classes. (class: %s)",
          ModelView.class, viewElement.getSimpleName());
      return false;
    }

    // Nested classes must be static
    if (((TypeElement) viewElement).getNestingKind().isNested()) {
      errorLogger.logError(
          "Classes with %s annotations cannot be nested. (class: %s)",
          ModelView.class, viewElement.getSimpleName());
      return false;
    }

    if (!isSubtypeOfType(viewElement.asType(), ProcessorUtils.ANDROID_VIEW_TYPE)) {
      errorLogger.logError(
          "Classes with %s annotations must extend android.view.View. (class: %s)",
          ModelView.class, viewElement.getSimpleName());
      return false;
    }

    return true;
  }

  private void processSetterAnnotations(RoundEnvironment roundEnv) {
    for (Element propMethod : roundEnv.getElementsAnnotatedWith(ModelProp.class)) {
      if (!validatePropElement(propMethod)) {
        continue;
      }

      ModelViewInfo info = getModelInfoForMethodElement(propMethod);
      if (info == null) {
        errorLogger.logError("%s annotation can only be used in classes annotated with %s",
            ModelProp.class, ModelView.class);
        continue;
      }

      info.addProp((ExecutableElement) propMethod);
    }
    addStringResOverloads();
  }

  /**
   * Add a StringRes attribute if an existing attribute type is String or CharSequence and there
   * isn't already a string res setter with the same name.
   */
  private void addStringResOverloads() {
    for (ModelViewInfo viewInfo : modelClassMap.values()) {
      List<ViewAttributeStringResOverload> overloads = new ArrayList<>();

      for (AttributeInfo attributeInfo : viewInfo.attributeInfo) {
        if (isType(elements, types, attributeInfo.typeMirror, String.class, CharSequence.class)) {

          AttributeInfo matcher = new AttributeInfoMatcher(attributeInfo.name, TypeName.INT);
          if (!viewInfo.attributeInfo.contains(matcher)) {
            // Add the new attribute after the for loop to prevent concurrent modification
            overloads.add(
                new ViewAttributeStringResOverload(viewInfo, (ViewAttributeInfo) attributeInfo,
                    types));
          }
        }
      }

      for (ViewAttributeStringResOverload overload : overloads) {
        viewInfo.attributeInfo.add(overload);
      }
    }
  }

  private boolean validatePropElement(Element methodElement) {
    return validateExecutableElement(methodElement, ModelProp.class, 1);
  }

  private boolean validateExecutableElement(Element element, Class<?> annotationClass,
      int paramCount) {
    if (!(element instanceof ExecutableElement)) {
      errorLogger.logError("%s annotations can only be on a method (element: %s)", annotationClass,
          element.getSimpleName());
      return false;
    }

    ExecutableElement executableElement = (ExecutableElement) element;
    if (executableElement.getParameters().size() != paramCount) {
      errorLogger.logError("Methods annotated with %s must have exactly %s parameter (method: %s)",
          annotationClass, paramCount, element.getSimpleName());
      return false;
    }

    Set<Modifier> modifiers = element.getModifiers();
    if (modifiers.contains(STATIC) || modifiers.contains(PRIVATE)) {
      errorLogger.logError("Methods annotated with %s cannot be private or static (method: %s)",
          annotationClass, element.getSimpleName());
      return false;
    }

    return true;
  }

  private void processResetAnnotations(RoundEnvironment roundEnv) {
    for (Element resetMethod : roundEnv.getElementsAnnotatedWith(ResetView.class)) {
      if (!validateResetElement(resetMethod)) {
        continue;
      }

      ModelViewInfo info = getModelInfoForMethodElement(resetMethod);
      if (info == null) {
        errorLogger.logError("%s annotation can only be used in classes annotated with %s",
            ModelProp.class, ModelView.class);
        continue;
      }

      info.addResetMethod((ExecutableElement) resetMethod);
    }
  }

  private boolean validateResetElement(Element resetMethod) {
    return validateExecutableElement(resetMethod, ResetView.class, 0);
  }

  private void writeJava() {
    for (final ModelViewInfo modelViewInfo : modelClassMap.values()) {
      try {
        modelWriter.generateClassForModel(modelViewInfo, new BuilderHooks() {
          @Override
          boolean addToBindMethod(Builder methodBuilder, ParameterSpec boundObjectParam) {
            for (AttributeInfo attributeInfo : modelViewInfo.attributeInfo) {
              methodBuilder.addStatement("$L.$L($L)", boundObjectParam.name, attributeInfo.getName())
            }
            return true;
          }

          @Override
          boolean addToBindWithDiffMethod(Builder methodBuilder, ParameterSpec boundObjectParam,
              ParameterSpec previousModelParam) {
            return super
                .addToBindWithDiffMethod(methodBuilder, boundObjectParam, previousModelParam);
          }

          @Override
          void addToUnbindMethod(MethodSpec.Builder unbindBuilder, String unbindParamName) {
            addResetMethodsToBuilder(unbindBuilder, modelViewInfo, unbindParamName);
          }
        });
      } catch (Exception e) {
        errorLogger.logError(new EpoxyProcessorException("eli error"),
            "Error generating model view classes");
      }
    }
  }

  private void addResetMethodsToBuilder(Builder builder, ModelViewInfo modelViewInfo,
      String unbindParamName) {
    for (String methodName : modelViewInfo.getResetMethodNames()) {
      builder.addStatement(unbindParamName + "." + methodName + "()");
    }
  }

  private ModelViewInfo getModelInfoForMethodElement(Element element) {
    Element enclosingElement = element.getEnclosingElement();
    if (enclosingElement == null) {
      return null;
    }

    return modelClassMap.get(enclosingElement);
  }
}
