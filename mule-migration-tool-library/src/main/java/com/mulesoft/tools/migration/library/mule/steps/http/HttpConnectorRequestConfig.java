/*
 * Copyright (c) 2017 MuleSoft, Inc. This software is protected under international
 * copyright law. All use of this software is subject to MuleSoft's Master Subscription
 * Agreement (or other master license agreement) separately entered into in writing between
 * you and MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package com.mulesoft.tools.migration.library.mule.steps.http;

import static com.mulesoft.tools.migration.step.category.MigrationReport.Level.WARN;

import com.mulesoft.tools.migration.step.AbstractApplicationModelMigrationStep;
import com.mulesoft.tools.migration.step.category.MigrationReport;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * Migrates the request configuration of the HTTP Connector
 *
 * @author Mulesoft Inc.
 * @since 1.0.0
 */
public class HttpConnectorRequestConfig extends AbstractApplicationModelMigrationStep {

  private static final String HTTP_NAMESPACE = "http://www.mulesoft.org/schema/mule/http";
  private static final String TLS_NAMESPACE = "http://www.mulesoft.org/schema/mule/tls";
  private static final String TCP_NAMESPACE = "http://www.mulesoft.org/schema/mule/tcp";
  private static final String SOCKETS_NAMESPACE = "http://www.mulesoft.org/schema/mule/sockets";

  public static final String XPATH_SELECTOR = ""
      + "/mule:mule/http:*["
      + " local-name()='request-config' or"
      + " local-name()='proxy' or"
      + " local-name()='ntlm-proxy'"
      + "]";

  @Override
  public String getDescription() {
    return "Update HTTP Connector request config.";
  }

  public HttpConnectorRequestConfig() {
    this.setAppliedTo(XPATH_SELECTOR);
  }

  @Override
  public void execute(Element object, MigrationReport report) throws RuntimeException {
    final Namespace httpNamespace = Namespace.getNamespace("http", HTTP_NAMESPACE);
    object.setNamespace(httpNamespace);

    if ("request-config".equals(object.getName())) {
      final Element requestConnection = new Element("request-connection", httpNamespace);
      copyAttributeIfPresent(object, requestConnection, "protocol");
      copyAttributeIfPresent(object, requestConnection, "host", "host", true);
      copyAttributeIfPresent(object, requestConnection, "port", "port", true);
      copyAttributeIfPresent(object, requestConnection, "usePersistentConnections");
      copyAttributeIfPresent(object, requestConnection, "maxConnections");
      copyAttributeIfPresent(object, requestConnection, "connectionIdleTimeout");
      copyAttributeIfPresent(object, requestConnection, "streamResponse");
      copyAttributeIfPresent(object, requestConnection, "responseBufferSize");
      copyAttributeIfPresent(object, requestConnection, "tlsContext-ref", "tlsContext", false);
      copyAttributeIfPresent(object, requestConnection, "clientSocketProperties-ref", "clientSocketProperties", false);
      copyAttributeIfPresent(object, requestConnection, "proxy-ref", "proxyConfig", false);

      object.addContent(requestConnection);

      for (Attribute attribute : object.getAttributes()) {
        if ("basePath".equals(attribute.getName())
            || "followRedirects".equals(attribute.getName())
            || "sendBodyMode".equals(attribute.getName())
            || "requestStreamingMode".equals(attribute.getName())
            || "responseTimeout".equals(attribute.getName())) {
          attribute.setValue(getExpressionMigrator().migrateExpression(attribute.getValue()));
        }
      }
    }

    object.getChildren().forEach(c -> {
      if (HTTP_NAMESPACE.equals(c.getNamespaceURI())) {
        execute(c, report);
      } else if (TLS_NAMESPACE.equals(c.getNamespaceURI()) && "context".equals(c.getName())) {
        final Element requestConnection = c.getParentElement().getChild("request-connection", httpNamespace);
        c.getParentElement().removeContent(c);
        requestConnection.addContent(c);
      } else if (TCP_NAMESPACE.equals(c.getNamespaceURI()) && "client-socket-properties".equals(c.getName())) {
        final Element clientSocketPropsContainer = new Element("client-socket-properties", httpNamespace);
        final Element requestConnection = c.getParentElement().getChild("request-connection", httpNamespace);

        c.getParentElement().removeContent(c);
        clientSocketPropsContainer.addContent(c);
        requestConnection.addContent(clientSocketPropsContainer);
      }

    });

    if ("basic-authentication".equals(object.getName())
        || "digest-authentication".equals(object.getName())
        || "ntlm-authentication".equals(object.getName())) {
      final Element authentication = new Element("authentication", httpNamespace);
      final Element requestConnection = object.getParentElement().getChild("request-connection", httpNamespace);

      object.getParentElement().removeContent(object);
      authentication.addContent(object);
      requestConnection.addContent(authentication);

      for (Attribute attribute : object.getAttributes()) {
        attribute.setValue(getExpressionMigrator().migrateExpression(attribute.getValue()));
      }
    }

    if (("proxy".equals(object.getName())
        || "ntlm-proxy".equals(object.getName()))
        && "request-config".equals(object.getParentElement().getName())) {
      final Element proxyConfig = new Element("proxy-config", httpNamespace);
      final Element requestConnection = object.getParentElement().getChild("request-connection", httpNamespace);

      object.getParentElement().removeContent(object);
      proxyConfig.addContent(object);
      requestConnection.addContent(proxyConfig);
    }

    if ("raml-api-configuration".equals(object.getName())) {
      report.report(WARN, object, object.getParentElement(),
                    "For consuming an API described by a RAML file, Rest-Connect is a more appropriate tool than using the HTTP Connector directly.",
                    "https://docs.mulesoft.com/anypoint-exchange/to-deploy-using-rest-connect");
      object.getParentElement().removeContent(object);
    }
  }

  protected void copyAttributeIfPresent(final Element source, final Element target, final String attributeName) {
    copyAttributeIfPresent(source, target, attributeName, attributeName, false);
  }

  protected void copyAttributeIfPresent(final Element source, final Element target, final String sourceAttributeName,
                                        final String targetAttributeName, boolean expression) {
    if (source.getAttribute(sourceAttributeName) != null) {
      target.setAttribute(targetAttributeName,
                          expression ? getExpressionMigrator().migrateExpression(source.getAttributeValue(sourceAttributeName))
                              : source.getAttributeValue(sourceAttributeName));
      source.removeAttribute(sourceAttributeName);
    }
  }
}