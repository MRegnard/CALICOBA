package fr.irit.smac.calicoba.gaml;

import java.util.Map;
import java.util.function.Function;

import msi.gama.metamodel.agent.IAgent;
import msi.gaml.descriptions.ConstantExpressionDescription;
import msi.gaml.statements.Arguments;
import msi.gaml.statements.IStatement;

public final class Utils {
  /**
   * Calls an action on the given agent.
   * 
   * @param <T>             Return type of action.
   * @param actionName      Action’s name.
   * @param agent           The agent to use.
   * @param parameterValues Values of each parameters of the action.
   * @return The value returned by the action.
   */
  public static <T> T callAction(String actionName, IAgent agent, Map<String, ? extends Object> parameterValues) {
    return callAction(actionName, agent, parameterValues, Function.identity());
  }

  /**
   * Calls an action on the given agent.
   * 
   * @param <T>             Return type of action.
   * @param actionName      Action’s name.
   * @param agent           The agent to use.
   * @param parameterValues Values of each parameters of the action.
   * @param paramNameMapper A function to apply on each map key to get the
   *                        corresponding action parameter name.
   * @return The value returned by the action.
   */
  @SuppressWarnings("unchecked")
  public static <T> T callAction(String actionName, IAgent agent, Map<String, ? extends Object> parameterValues,
      Function<String, String> paramNameMapper) {
    IStatement.WithArgs action = agent.getSpecies().getAction(actionName);
    Arguments actionArgs = new Arguments();
    parameterValues.entrySet().forEach(e -> {
      String paramName = e.getKey();
      Object paramValue = parameterValues.get(paramName);
      actionArgs.put(paramNameMapper.apply(paramName), ConstantExpressionDescription.create(paramValue));
    });
    return (T) agent.getScope().execute(action, actionArgs).getValue();
  }

  private Utils() {
  }
}
