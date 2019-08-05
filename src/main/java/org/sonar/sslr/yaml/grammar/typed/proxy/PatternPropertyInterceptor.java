package org.sonar.sslr.yaml.grammar.typed.proxy;

public class PatternPropertyInterceptor extends MapProxyFactory implements PropertyPointer {
  private final String methodName;

  public PatternPropertyInterceptor(String methodName, String pattern, ProxyFactory elementFactory) {
    super(pattern, elementFactory);
    this.methodName = methodName;
  }

  @Override
  public String getMethodName() {
    return methodName;
  }
}
