package org.sonar.sslr.yaml.grammar.typed.proxy;

import org.sonar.sslr.yaml.grammar.JsonNode;

public class ListProxyFactory implements ProxyFactory {
  private final ProxyFactory elementFactory;

  public ListProxyFactory(ProxyFactory elementFactory) {
    this.elementFactory = elementFactory;
  }

  @Override
  public Object makeProxyFor(JsonNode node) {
    return new ListProxy(node, elementFactory);
  }
}
