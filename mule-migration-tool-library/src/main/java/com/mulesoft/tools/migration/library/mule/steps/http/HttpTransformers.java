/*
 * Copyright (c) 2017 MuleSoft, Inc. This software is protected under international
 * copyright law. All use of this software is subject to MuleSoft's Master Subscription
 * Agreement (or other master license agreement) separately entered into in writing between
 * you and MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package com.mulesoft.tools.migration.library.mule.steps.http;

import static com.mulesoft.tools.migration.step.category.MigrationReport.Level.WARN;
import static com.mulesoft.tools.migration.step.util.XmlDslUtils.CORE_NAMESPACE;
import static com.mulesoft.tools.migration.step.util.XmlDslUtils.VALIDATION_NAMESPACE;
import static com.mulesoft.tools.migration.step.util.XmlDslUtils.addElementAfter;
import static com.mulesoft.tools.migration.step.util.XmlDslUtils.addValidationModule;

import com.mulesoft.tools.migration.step.AbstractApplicationModelMigrationStep;
import com.mulesoft.tools.migration.step.ExpressionMigratorAware;
import com.mulesoft.tools.migration.step.category.ExpressionMigrator;
import com.mulesoft.tools.migration.step.category.MigrationReport;

import org.jdom2.Element;

/**
 * Migrates the transformers of the http transport
 *
 * @author Mulesoft Inc.
 * @since 1.0.0
 */
public class HttpTransformers extends AbstractApplicationModelMigrationStep
    implements ExpressionMigratorAware {

  public static final String XPATH_SELECTOR =
      "//http:*[local-name()='body-to-parameter-map-transformer' or "
          + "local-name()='request-wildcard-filter' or "
          + "local-name()='http-response-to-object-transformer' or "
          + "local-name()='http-response-to-string-transformer' or "
          + "local-name()='object-to-http-request-transformer' or "
          + "local-name()='message-to-http-response-transformer' or "
          + "local-name()='body-to-parameter-map-transformer']";

  private ExpressionMigrator expressionMigrator;

  @Override
  public String getDescription() {
    return "Update HTTP tranformers.";
  }

  public HttpTransformers() {
    this.setAppliedTo(XPATH_SELECTOR);
  }

  @Override
  public void execute(Element object, MigrationReport report) throws RuntimeException {
    if ("request-wildcard-filter".equals(object.getName())) {
      addValidationModule(getApplicationModel());

      object.setName("matches-regex");
      object.setNamespace(VALIDATION_NAMESPACE);

      String regex;
      Element parent = object.getParentElement();
      if ("not-filter".equals(parent.getName()) && parent.getNamespace().equals(CORE_NAMESPACE)) {
        object.detach();
        addElementAfter(object, parent);
        parent.detach();

        regex = "^(?!" + object.getAttributeValue("pattern").replaceAll("\\*", ".*") + ").*$";
      } else {
        regex = "^" + object.getAttributeValue("pattern").replaceAll("\\*", ".*") + "$";
      }

      object.setAttribute("value", "#[message.attributes.requestPath]");
      object.setAttribute("regex", regex);
      object.removeAttribute("pattern");
    } else if ("body-to-parameter-map-transformer".equals(object.getName())) {
      report.report(WARN, object, object.getParentElement(),
                    "This transformer is no longer required since DataWeave can handle this kind of payloads.");
      object.getParent().removeContent(object);
    } else {
      object.getParent().removeContent(object);
    }
  }

  @Override
  public void setExpressionMigrator(ExpressionMigrator expressionMigrator) {
    this.expressionMigrator = expressionMigrator;
  }

  @Override
  public ExpressionMigrator getExpressionMigrator() {
    return expressionMigrator;
  }

}