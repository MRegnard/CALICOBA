package fr.irit.smac.calicoba.experiments.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.irit.smac.calicoba.experiments.Model;

/**
 * This factory builds procedural models composed of layers of operator nodes
 * (sum, product, division and delay).
 * 
 * @note L. Pons, “Self-tuning of game scenarios through self-adaptive
 *       multi-agent systems”, 2014.
 */
public class ModelFactory {
  private final List<Function<Integer, Node>> nodeTypes;
  private final Random rng;

  /**
   * Creates a new model factory using the given node types.
   * 
   * @param nodeTypes Node types to use when building new models.
   */
  public ModelFactory(Set<Class<? extends Node>> nodeTypes) {
    if (nodeTypes.isEmpty()) {
      throw new IllegalArgumentException("set is empty");
    }
    this.rng = new Random();
    this.nodeTypes = nodeTypes.stream().<Function<Integer, Node>>map(c -> (branch -> {
      try {
        return c.getConstructor(int.class).newInstance(branch);
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
          | NoSuchMethodException | SecurityException e) {
        throw new RuntimeException(e);
      }
    })).collect(Collectors.toList());
  }

  /**
   * Sets the seed of this factory’s RNG.
   * 
   * @param seed The new seed.
   */
  public void setSeed(long seed) {
    this.rng.setSeed(seed);
  }

  /**
   * Generates a model composed of several layers of operator nodes.
   * 
   * @param layersNb  The number of node layers.
   * @param outputsNb The number of outputs.
   * @return The resulting model.
   */
  public Model generateModel(final int layersNb, final int outputsNb, final int branchingFactor) {
    if (branchingFactor < 1 || branchingFactor > outputsNb) {
      throw new IllegalArgumentException(
          String.format("branching factor must be in [1, %d], got %d", outputsNb, branchingFactor));
    }

    Map<String, Node> outputNodes = new HashMap<>();
    Map<String, ParameterNode> parameters = new HashMap<>();

    // Generate outputs
    List<Node> currentLayer = new ArrayList<>();
    for (int i = 0; i < outputsNb; i++) {
      Node node = this.getRandomNode(i);
      currentLayer.add(node);
      outputNodes.put("o" + (i + 1), node);
    }

    int inputsNb = 0;
    // Generate intermediate layers
    for (int l = 0; l < layersNb; l++) {
      List<Node> nextLayer = new ArrayList<>(currentLayer.size());
      for (Node node : currentLayer) {
        for (int i = 0; i < node.getInputsNumber(); i++) {
          Node parent = this.getRandomNode(node.getBranch());
          nextLayer.add(parent);
          node.setParentNode(i, parent);
          if (l == layersNb - 1) {
            inputsNb += parent.getInputsNumber();
          }
        }
      }
      currentLayer = nextLayer;
    }

    // Generate parameter nodes
    Map<Integer, List<Node>> branches = currentLayer.stream().collect(Collectors.groupingBy(Node::getBranch));
    int parameterID = 1;
    // Rules:
    // - A parameter can connect at most once on each branch.
    // - A parameter cannot connect to more inputs than its branching factor.
    // - If an input cannot find a free parameter, a new one is created.
    // - Repeat until all inputs have found a parameter.
    while (inputsNb != 0) {
      String pName = "p" + parameterID;
      ParameterNode parameter = new ParameterNode(pName, branchingFactor);
      parameters.put(pName, parameter);
      parameterID++;

      boolean paramDone = false;
      for (int i = 0; !paramDone && i < outputsNb; i++) {
        List<Node> branch = branches.get(i);
        boolean parentSet = false;

        for (int j = 0; !parentSet && j < branch.size(); j++) {
          Node node = branch.get(j);
          for (int k = 0; !parentSet && k < node.getInputsNumber(); k++) {
            if (!node.isParentSet(k)) {
              node.setParentNode(k, parameter);
              parameter.decreaseBranchingFactor();
              inputsNb--;
              parentSet = true;
              if (parameter.getBranchingFactor() == 0) {
                paramDone = true;
              }
            }
          }
        }
      }
    }

    return new GeneratedModel(parameters, outputNodes);
  }

  /**
   * Returns a random node with no parents.
   */
  private Node getRandomNode(final int branch) {
    int index = this.rng.nextInt(this.nodeTypes.size());
    return this.nodeTypes.get(index).apply(branch);
  }

  public static void main(String[] args) { // TEST
    ModelFactory mf = new ModelFactory(
        new HashSet<>(Arrays.asList(SumNode.class, ProductNode.class, DivisionNode.class)));
    mf.setSeed(2);
    Model model = mf.generateModel(1, 2, 2);
    System.out.println(model);

    Map<String, Double> targetParameters = model.getParameterNames().stream()
        .collect(Collectors.toMap(Function.identity(), k -> Math.floor(Math.random() * 100)));
    Map<String, Double> targetOutputs = model.evaluate(targetParameters);
    System.out.println(targetParameters);
    System.out.println(targetOutputs);
  }
}
