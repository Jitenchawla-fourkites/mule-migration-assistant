package com.mulesoft.tools.migration.task.step;

import com.mulesoft.tools.migration.exception.MigrationStepException;
import org.apache.commons.lang3.ArrayUtils;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mulesoft Inc.
 * @since 2.0.0
 */
public class MoveMUnitProcessors extends MigrationStep {

    private static String[] sections = new String[]{"munit:behavior","munit:execution","munit:validation"};

    public MoveMUnitProcessors(){}

    @Override
    public void execute() throws Exception {
        try {
            for (Element element : getNodes()) {

                List<Element> childNodes = element.getChildren();
                CreateBehaviorSection(childNodes, element);
                CreateExecutionSection(childNodes, element);
                CreateValidationSection(childNodes, element);
            }
        } catch (Exception ex) {
            throw new MigrationStepException("Update Mule Message Content exception. " + ex.getMessage());
        }

    }


    public void CreateBehaviorSection(List<Element> nodes, Element parent){

        int pos = 0;
        List<Element> behaviorNodes = new ArrayList<>();
        while (nodes.size() > pos &&
                !nodes.get(pos).getQualifiedName().equals("flow-ref") &&
                !nodes.get(pos).getQualifiedName().equals("munit-tools:assert-that") &&
                !ArrayUtils.contains(sections, nodes.get(pos).getQualifiedName())) {
            behaviorNodes.add(nodes.get(pos));
            pos++;
        }
        CreateChildWithElements("behavior", behaviorNodes, parent);
    }


    public void CreateExecutionSection(List<Element> nodes, Element parent){

        int pos = 0;
        List<Element> behaviorNodes = new ArrayList<>();
        while (nodes.size() > pos &&
                !nodes.get(pos).getQualifiedName().equals("munit-tools:assert-that") &&
                !ArrayUtils.contains(sections, nodes.get(pos).getQualifiedName())) {
            behaviorNodes.add(nodes.get(pos));
            pos++;
        }
        CreateChildWithElements("execution", behaviorNodes, parent);
    }

    public void CreateValidationSection(List<Element> nodes, Element parent){

        int pos = 0;
        List<Element> behaviorNodes = new ArrayList<>();
        while (nodes.size() > pos &&
                !ArrayUtils.contains(sections, nodes.get(pos).getQualifiedName())) {
            behaviorNodes.add(nodes.get(pos));
            pos++;
        }
        CreateChildWithElements("validation", behaviorNodes, parent);
    }


    private void CreateChildWithElements(String childName, List<Element> elements, Element parent) {

        if (elements.size() > 0) {

            elements.forEach(parent::removeContent);

            Element section = new Element(childName);
            section.setNamespace(parent.getNamespace());

            section.addContent(elements);
            parent.addContent(section);
        }
    }
}