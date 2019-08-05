package org.sonar.sslr.yaml.grammar.typed.proxy;

import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.PropertyDescription;
import org.sonar.sslr.yaml.snakeyaml.parser.Tokens;

class PropertyInterceptor implements ProxyFactory, PropertyPointer {
  private final String methodName;
  private final String propertyName;
  private final ProxyFactory factory;

  PropertyInterceptor(String methodName, String propertyName, ProxyFactory factory) {
    this.methodName = methodName;
    this.propertyName = propertyName;
    this.factory = factory;
  }

  @Override
  public Object makeProxyFor(JsonNode parent) {
    JsonNode node = parent.get(propertyName);
    if (node.isMissing() || node.is(Tokens.NULL)) {
      return null;
    }
    return factory.makeProxyFor(node);
  }

  @Override
  public String getMethodName() {
    return methodName;
  }
}
