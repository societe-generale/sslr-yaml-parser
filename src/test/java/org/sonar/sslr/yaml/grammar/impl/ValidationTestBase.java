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
package org.sonar.sslr.yaml.grammar.impl;

import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.impl.LexerException;
import java.nio.charset.Charset;
import org.junit.Before;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.ASTValidator;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.ValidationRule;
import org.sonar.sslr.yaml.grammar.YamlParser;

public class ValidationTestBase {
  protected static final GrammarRuleKey FAKE_RULE = new GrammarRuleKey() {};
  protected ValidationRule.Context context;

  @Before
  public void createContext() {
    context = new ASTValidator.ContextImpl();
    context.capture();
  }

  protected final JsonNode parseText(String text) {
    try {
      return new YamlParser(Charset.forName("UTF-8"), null, true).parse(text);
    } catch (LexerException e) {
      throw new RecognitionException(e);
    }
  }
}
