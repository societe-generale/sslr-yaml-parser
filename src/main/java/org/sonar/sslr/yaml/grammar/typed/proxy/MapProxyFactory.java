package org.sonar.sslr.yaml.grammar.typed.proxy;

import org.sonar.sslr.yaml.grammar.JsonNode;

public class MapProxyFactory implements ProxyFactory {
  private final String pattern;
  private final ProxyFactory elementFactory;

  public MapProxyFactory(String pattern, ProxyFactory elementFactory) {
    this.pattern = pattern;
    this.elementFactory = elementFactory;
  }

  @Override
  public Object makeProxyFor(JsonNode node) {
    return new MapProxy(node, pattern, elementFactory);
  }
}
