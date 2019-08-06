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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import org.sonar.sslr.grammar.GrammarRuleKey;

@FunctionalInterface
public interface TypeVisitor {
   Object visit(Type type, Annotation... annotations);

   public interface Context {
     GrammarRuleKey makeTypeKey(Type type);

     /**
      * Registers the key in the context.
      * @param  ruleKey a rule to declare
      * @return {@code false} if the key was already registered.
      */
     boolean add(GrammarRuleKey ruleKey);

     void declareMethod(String name);

     void declareTypes(Class types);
   }
}
