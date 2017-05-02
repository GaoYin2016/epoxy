package com.airbnb.epoxy;

import com.airbnb.epoxy.GeneratedModelWriter.BeforeBuildCallback;
import com.squareup.javapoet.TypeSpec.Builder;

import java.util.Collection;
import java.util.LinkedHashMap;
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
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

class ModelViewProcessor {
  private final Elements elementUtils;
  private final Types typeUtils;
  private final ConfigManager configManager;
  private final ErrorLogger errorLogger;
  private final GeneratedModelWriter modelWriter;
  private final Map<Element, ModelViewInfo> modelClassMap = new LinkedHashMap<>();

  ModelViewProcessor(Elements elementUtils, Types typeUtils, ConfigManager configManager,
      ErrorLogger errorLogger, GeneratedModelWriter modelWriter) {

    this.elementUtils = elementUtils;
    this.typeUtils = typeUtils;
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
            new ModelViewInfo((TypeElement) viewElement, typeUtils, elementUtils));
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
  }

  private boolean validatePropElement(Element methodElement) {
    return validateExecutableElement(methodElement, ModelProp.class, 1);
  }

  private boolean validateExecutableElement(Element element, Class<?> annotationClass,
      int paramCount) {
    if (!(element instanceof ExecutableElement)) {
      errorLogger.logError("%s annotations can only be on a method (element: %s)", annotationClass,
          element.getSimpleName());
      return true;
    }

    ExecutableElement executableElement = (ExecutableElement) element;
    if (executableElement.getParameters().size() != paramCount) {
      errorLogger.logError("Methods annotated with %s must have exactly %s parameter (method: %s)",
          annotationClass, paramCount, element.getSimpleName());
      return true;
    }

    Set<Modifier> modifiers = element.getModifiers();
    if (modifiers.contains(STATIC) || modifiers.contains(PRIVATE)) {
      errorLogger.logError("Methods annotated with %s cannot be private or static (method: %s)",
          annotationClass, element.getSimpleName());
      return true;
    }

    return false;
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
        modelWriter.generateClassForModel(modelViewInfo, new BeforeBuildCallback() {
          @Override
          public void modifyBuilder(Builder builder) {
            addResetMethodsToBuilder(builder, modelViewInfo);
          }
        });
      } catch (Exception e) {
        errorLogger.logError(new EpoxyProcessorException("eli error"),
            "Error generating model view classes");
      }
    }
  }

  private void addResetMethodsToBuilder(Builder builder, ModelViewInfo modelViewInfo) {
    for (String methodName : modelViewInfo.getResetMethodNames()) {

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
