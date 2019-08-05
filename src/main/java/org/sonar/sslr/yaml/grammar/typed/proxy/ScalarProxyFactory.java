package org.sonar.sslr.yaml.grammar.typed.proxy;

import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.snakeyaml.parser.Tokens;

import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.FLOAT;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.INTEGER;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.STRING;

public class ScalarProxyFactory implements ProxyFactory {
  @Override
  public Object makeProxyFor(JsonNode node) {
    if (node.is(STRING)) {
      return node.stringValue();
    } else if (node.is(FLOAT)) {
      return node.floatValue();
    } else if (node.is(INTEGER)) {
      return node.intValue();
    } else if (node.is(Tokens.TRUE)) {
      return true;
    } else if (node.is(Tokens.FALSE)) {
      return false;
    } else {
      return null;
    }
  }
}
