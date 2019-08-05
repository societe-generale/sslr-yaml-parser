package org.sonar.sslr.yaml.grammar.typed.proxy;

import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.snakeyaml.parser.Tokens;

public class NumberProxyFactory implements ProxyFactory {
  @Override
  public Object makeProxyFor(JsonNode node) {
    if (node.is(Tokens.FLOAT))
      return node.floatValue();
    else
      return node.intValue();
  }
}
