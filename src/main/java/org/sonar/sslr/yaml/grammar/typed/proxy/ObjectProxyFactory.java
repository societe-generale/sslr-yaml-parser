package org.sonar.sslr.yaml.grammar.typed.proxy;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.sonar.sslr.yaml.grammar.JsonNode;

public class ObjectProxyFactory implements ProxyFactory {
  private final Map<String, ProxyFactory> methods = new HashMap<>();
  private final Class[] types;

  public ObjectProxyFactory(Class[] types) {
    this.types = types;
  }

  public void addProperty(PropertyPointer pointer) {
    methods.put(pointer.getMethodName(), pointer);
  }

  @Override
  public Object makeProxyFor(JsonNode node) {
    return Proxy.newProxyInstance(getClass().getClassLoader(), types, new ObjectProxy(node, Collections.unmodifiableMap(methods)));
  }

}
