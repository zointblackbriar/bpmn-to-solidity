package com.github.rasika.bpmn2solidity.tree.model.toplevel;

import com.github.rasika.bpmn2solidity.solidty.SolidityCodeTemplate;
import com.github.rasika.bpmn2solidity.solidty.SolidityConstructor;
import com.github.rasika.bpmn2solidity.solidty.SolidityContract;
import com.github.rasika.bpmn2solidity.solidty.SolidityFunction;
import com.github.rasika.bpmn2solidity.solidty.SolidityInstruction;
import com.github.rasika.bpmn2solidity.tree.TreeBuilder;
import com.github.rasika.bpmn2solidity.tree.model.Node;
import com.github.rasika.bpmn2solidity.tree.model.Process;
import com.github.rasika.bpmn2solidity.tree.model.Property;
import com.github.rasika.bpmn2solidity.tree.model.SolidityNode;
import com.github.rasika.bpmn2solidity.tree.model.task.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Message extends SolidityNode {
    public List<Property> properties = new ArrayList<>();
    public Process parentProcess;
    public String parentId;
    private boolean topLevelTaskExplicitlySet;

    public Message(Node currentNode) {
        super(currentNode);
        getChildren().forEach(child -> {
            if ("property".equals(child.type)) {
                properties.add(TreeBuilder.createProperty(child));
            }
        });
    }

    @Override
    public void relink(Map<String, Node> idToNode) {
        for (Property property : properties) {
            property.relink(idToNode);
        }
    }

    @Override
    public void setTopLevelTask(Task topLevelTask) {
        topLevelTaskExplicitlySet = true;
        super.setTopLevelTask(topLevelTask);
    }

    @Override
    public void toSolidity(SolidityCodeTemplate template) {
        if (parentProcess != null) {
            SolidityContract contract = template.getContract(parentProcess.contractName);
            if (parentId != null) {
                Task topLevelTask;
                if ((topLevelTask = getTopLevelTask()) != null) {
                    SolidityFunction function = contract.getFunction(topLevelTask.functionName);
                    String params = getParams(properties);
                    function.addInstruction(new SolidityInstruction("emit " + name + "(" + params + ");"));
                    contract.addEvent(name, getDefs(properties));
                } else if (topLevelTaskExplicitlySet) {
                    SolidityConstructor constructor = contract.getConstructor();
                    String params = getParams(properties);
                    constructor.addInstruction(new SolidityInstruction("emit " + name + "(" + params + ");"));
                    contract.addEvent(name, getDefs(properties));
                }
            }
        }
    }

    private String getParams(List<Property> properties) {
        return properties.stream()
                .map(p -> {
                    String val = p.toSolidity().split(" ")[1];
                    String[] parts = val.split("=");
                    return (parts.length > 1) ? parts[1] : parts[0];
                })
                .collect(Collectors.joining(", "));
    }

    private List<String> getDefs(List<Property> properties) {
        return properties.stream()
                .map(p -> p.toSolidity().split("=")[0])
                .collect(Collectors.toList());
    }
}
