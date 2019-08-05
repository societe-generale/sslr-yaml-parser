package org.sonar.sslr.yaml.grammar.typed.proxy;

import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.snakeyaml.parser.Tokens;

public class IdentityProxyFactory implements ProxyFactory {
  @Override
  public Object makeProxyFor(JsonNode node) {
    if (node.isMissing() || node.is(Tokens.NULL)) {
      return null;
    } else {
      return node;
    }
  }
}
