package org.sonar.sslr.yaml.grammar.typed.proxy;

import org.sonar.sslr.yaml.grammar.JsonNode;

public class IntegerProxyFactory implements ProxyFactory {
  @Override
  public Object makeProxyFor(JsonNode node) {
    return node.intValue();
  }
}
