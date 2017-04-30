package com.airbnb.epoxy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

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

    for (Element viewElement : roundEnv.getElementsAnnotatedWith(ModelView.class)) {
      try {
        if (!validateViewElement(viewElement)) {
          continue;
        }

        modelClassMap.put(viewElement,
            new ModelViewInfo((TypeElement) viewElement, typeUtils, elementUtils));
      } catch (Exception e) {
        errorLogger.logError(e);
      }
    }

    for (Element propMethod : roundEnv.getElementsAnnotatedWith(ModelProp.class)) {
      if (!validatePropElement(propMethod)) {
        continue;
      }

      ModelViewInfo info = getModelInfoForMethodElement(propMethod);
      if (info == null) {
        errorLogger.logError("%s annotation can only be used in classes annotated with %s",
            ModelProp.class, ModelView.class);
      }

      info.addProp((ExecutableElement) propMethod);
    }

    for (Element resetMethod : roundEnv.getElementsAnnotatedWith(ResetView.class)) {
      if (!validateResetElement(resetMethod)) {
        continue;
      }

      ModelViewInfo info = getModelInfoForMethodElement(resetMethod);
      if (info == null) {
        errorLogger.logError("%s annotation can only be used in classes annotated with %s",
            ModelProp.class, ModelView.class);
      }

      info.addResetMethod((ExecutableElement) resetMethod);
    }

    return modelClassMap.values();
  }

  private ModelViewInfo getModelInfoForMethodElement(Element element) {
    Element enclosingElement = element.getEnclosingElement();
    if (enclosingElement == null) {
      return null;
    }

    return modelClassMap.get(element);
  }

  private boolean validatePropElement(Element propMethod) {
    return true;
  }

  private boolean validateResetElement(Element resetMethod) {
    return true;
  }

  private boolean validateViewElement(Element viewElement) {
    return false;
  }
}
