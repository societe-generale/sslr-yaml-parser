package org.sonar.sslr.yaml.grammar.typed.proxy;

import org.sonar.sslr.yaml.grammar.JsonNode;

public class StringProxyFactory implements ProxyFactory {
  @Override
  public Object makeProxyFor(JsonNode node) {
    return node.stringValue();
  }
}
