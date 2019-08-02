package org.sonar.sslr.yaml.grammar.typed.impl2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.DefaultGrammarBuilder;
import org.sonar.sslr.yaml.grammar.typed.Resolvable;
import org.sonar.sslr.yaml.grammar.typed.TypeVisitor;

public class ResolvableVisitor implements TypeVisitor {
  private final DefaultGrammarBuilder builder;
  private final Context context;

  public ResolvableVisitor(DefaultGrammarBuilder builder, Context context) {
    this.builder = builder;
    this.context = context;
  }

  @Override
  public Object visit(Type type, Annotation... annotations) {
    GrammarRuleKey ruleKey = context.makeTypeKey(Resolvable.class);
    if (!context.add(ruleKey)) {
      return ruleKey;
    }
    builder.rule(ruleKey).is(builder.object(builder.mandatoryProperty("$ref", builder.string())));
    return ruleKey;
  }
}
