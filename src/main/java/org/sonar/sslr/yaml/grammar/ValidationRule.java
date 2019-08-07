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
package org.sonar.sslr.yaml.grammar;

import java.util.List;

public interface ValidationRule {
  /**
   * Validate the supplied node.
   * @param node the node to validate
   * @param context validation context
   * @return {@code true} if the node respects the rule
   */
  boolean visit(JsonNode node, Context context);

  interface Context {
    /**
     * Records and throws a violation of a rule.
     * @param node the location of the violation
     * @param message the violation description
     * @param causes any other violations that may have caused or explain this violation
     * @throws ValidationException in any case
     */
    void recordFailure(JsonNode node, String message, ValidationIssue... causes) throws ValidationException;

    void recordWarning(JsonNode node, String message, ValidationIssue... causes) throws ValidationException;

    void capture();

    List<ValidationIssue> captured();
  }
}
