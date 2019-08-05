package org.sonar.sslr.yaml.grammar.typed.proxy;

import java.util.Map;
import java.util.NoSuchElementException;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.typed.Resolvable;

public class ResolvableProxy extends ObjectProxy implements Resolvable {

  private JsonNode resolved;

  public ResolvableProxy(JsonNode node, Map<String, ProxyFactory> methods) {
    super(node, methods);
  }



}
