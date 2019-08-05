package org.sonar.sslr.yaml.grammar.typed;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.DefaultGrammarBuilder;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.ValidationIssue;
import org.sonar.sslr.yaml.grammar.YamlParser;
import org.sonar.sslr.yaml.grammar.typed.impl2.TypeDispatcher;
import org.sonar.sslr.yaml.grammar.typed.impl2.VisitorFactoryImpl;
import org.sonar.sslr.yaml.grammar.typed.proxy.ProxyFactory;
import org.sonar.sslr.yaml.grammar.typed.proxy.ProxyFactoryGenerator;

public class TypedYamlParser<T> {
  private final YamlParser parser;
  private final ProxyFactory proxyFactory;

  private TypedYamlParser(YamlParser parser, ProxyFactory proxyFactory) {
    this.parser = parser;
    this.proxyFactory = proxyFactory;
  }

  public T parse(File file) {
    JsonNode node = parser.parse(file);
    return (T) proxyFactory.makeProxyFor(node);
  }

  public T parse(String text) {
    JsonNode node = parser.parse(text);
    return (T) proxyFactory.makeProxyFor(node);
  }

  public List<ValidationIssue> getIssues() {
    return parser.getIssues();
  }

  public static <T> Builder<T> builder(Class<? extends T> rootClass) {
    return new Builder<>(rootClass);
  }

  public static final class Builder<T> {
    private YamlParser.Builder builder = YamlParser.builder();
    private ProxyFactory rootFactory;

    public Builder(Class<? extends T> rootClass) {
      this.withClass(rootClass);
    }

    public Builder<T> withCharset(Charset charset) {
      builder.withCharset(charset);
      return this;
    }

    public Builder<T> withStrictValidation(boolean validate) {
      builder.withStrictValidation(validate);
      return this;
    }

    private void withClass(Class<? extends T> rootClass) {
      DefaultGrammarBuilder grammar = new DefaultGrammarBuilder();

      ProxyFactoryGenerator proxyGenerator = new ProxyFactoryGenerator(grammar);
      VisitorFactoryImpl factory = new VisitorFactoryImpl(proxyGenerator);
      TypeDispatcher dispatcher = new TypeDispatcher(factory);
      factory.setDispatcher(dispatcher);

      GrammarRuleKey rootKey = (GrammarRuleKey) dispatcher.visit(rootClass);
      grammar.setRootRule(rootKey);

      builder.withGrammar(proxyGenerator.build());
      this.rootFactory = proxyGenerator.getRootFactory();
    }

    public TypedYamlParser<T> build() {
      return new TypedYamlParser<>(builder.build(), rootFactory);
    }
  }
}
