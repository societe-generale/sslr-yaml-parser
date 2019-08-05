package org.sonar.sslr.yaml.grammar.typed.proxy;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.impl.FirstOfValidation;

public class ChoiceProxyFactory implements ProxyFactory, FirstOfValidation.Observer {
  private final List<ProxyFactory> factories;
  private final Map<JsonNode, Integer> matchedNodes = new IdentityHashMap<>();

  public ChoiceProxyFactory(List<ProxyFactory> factories) {
    this.factories = factories;
  }
  @Override
  public Object makeProxyFor(JsonNode node) {
    Integer remove = matchedNodes.remove(node);
    if (remove == null) {
      throw new IllegalStateException("Node " + node + " has not been observed!");
    }
    return factories.get(remove).makeProxyFor(node);
  }

  @Override
  public void onObserved(JsonNode node, int i) {
    matchedNodes.put(node, i);
  }
}
