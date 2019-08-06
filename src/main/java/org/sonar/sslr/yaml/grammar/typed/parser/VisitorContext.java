/*
 * Sonar SSLR :: YAML Parser
 * Copyright (C) 2018-2019 Societe Generale
 * vincent.girard-reydet AT socgen DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.sslr.yaml.grammar.typed.parser;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sonar.sslr.grammar.GrammarRuleKey;

public class VisitorContext implements TypeVisitor.Context {
  private final Set<GrammarRuleKey> keys = new HashSet<>();

  @Override
  public GrammarRuleKey makeTypeKey(Type type) {
    return new StringRuleKey(type.getTypeName());
  }

  @Override
  public boolean add(GrammarRuleKey ruleKey) {
    return keys.add(ruleKey);
  }

  @Override
  public void declareMethod(String name) {

  }

  @Override
  public void declareTypes(Class types) {

  }

  private static class StringRuleKey implements GrammarRuleKey {
    private final String name;

    private StringRuleKey(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      StringRuleKey that = (StringRuleKey) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

}
