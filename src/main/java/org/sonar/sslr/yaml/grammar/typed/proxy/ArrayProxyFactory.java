package org.sonar.sslr.yaml.grammar.typed.proxy;

import java.lang.reflect.Array;
import java.util.List;
import org.sonar.sslr.yaml.grammar.JsonNode;

public class ArrayProxyFactory implements ProxyFactory {
  private final Class elementType;
  private final ProxyFactory elementFactory;

  public ArrayProxyFactory(Class elementType, ProxyFactory elementFactory) {
    this.elementType = elementType;
    this.elementFactory = elementFactory;
  }

  @Override
  public Object makeProxyFor(JsonNode node) {
    List<JsonNode> elements = node.elements();
    Object o = Array.newInstance(elementType, elements.size());
    for (int i=0; i < elements.size(); ++i) {
      JsonNode element = elements.get(i);
      Array.set(o, i, elementFactory.makeProxyFor(element));
    }
    return o;
  }
}
