package org.sonar.sslr.yaml.grammar.typed.proxy;

import java.util.Map;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.JsonNode;

public class DeferredProxyFactory implements ProxyFactory {
  private final Map<GrammarRuleKey, ProxyFactory> factories;
  private final GrammarRuleKey key;

  public DeferredProxyFactory(Map<GrammarRuleKey, ProxyFactory> factories, GrammarRuleKey key) {
    this.factories = factories;
    this.key = key;
  }

  @Override
  public Object makeProxyFor(JsonNode node) {
    ProxyFactory proxyFactory = factories.get(key);
    if (proxyFactory == null) {
      throw new IllegalStateException("Factory for type " + key + " has not been defined!");
    }
    return proxyFactory.makeProxyFor(node);
  }
}
