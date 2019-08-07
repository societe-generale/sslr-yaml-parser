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

import com.fasterxml.jackson.core.JsonPointer;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.sonar.sslr.yaml.grammar.impl.ArrayNode;
import org.sonar.sslr.yaml.grammar.impl.MissingNode;
import org.sonar.sslr.yaml.grammar.impl.ObjectNode;
import org.sonar.sslr.yaml.grammar.impl.PropertyNode;
import org.sonar.sslr.yaml.grammar.impl.ScalarNode;
import org.sonar.sslr.yaml.grammar.impl.SyntaxNode;

import static org.sonar.sslr.yaml.grammar.YamlGrammar.BLOCK_ARRAY_ELEMENT;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.BLOCK_MAPPING;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.BLOCK_PROPERTY;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.BLOCK_SEQUENCE;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.FLOW_ARRAY_ELEMENT;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.FLOW_MAPPING;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.FLOW_PROPERTY;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.FLOW_SEQUENCE;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.INDENTLESS_SEQUENCE;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.ROOT;
import static org.sonar.sslr.yaml.grammar.YamlGrammar.SCALAR;
import static org.sonar.sslr.yaml.grammar.impl.MissingNode.MISSING;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.KEY;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.VALUE;


public abstract class JsonNode extends AstNode {
  private final AstNodeType originalType;
  private String pointer;

  protected JsonNode(AstNodeType type, String name, @Nullable Token token) {
    super(type, name, token);
    originalType = type;
  }

  /**
   * Get the node pointed at by the supplied JSON pointer. The pointer must start with {@code /}.
   * @param path a valid JSON pointer string.
   * @return the matching child node, or the Missing node if not found.
   */
  public final JsonNode at(String path) {
    return at(JsonPointer.compile(path));
  }

  /**
   * Get the node pointed at by the supplied JSON pointer.
   * @param pointer a valid JSON pointer.
   * @return the matching child node, or the Missing node if not found.
   */
  public final JsonNode at(JsonPointer pointer) {
    if (pointer.matches()) {
      return this;
    } else {
      JsonNode n = _at(pointer);
      if (n == null) {
        return MISSING;
      }
      return n.at(pointer.tail());
    }
  }

  public final String getPointer() {
    if (this.pointer != null) {
      return this.pointer;
    }
    StringBuilder path = new StringBuilder();
    JsonNode root = this;
    while (root.getParent() != null && root.getParent().getType() != ROOT) {
      if (!root.isSyntax()) {
        JsonNode key = root.key();
        if (!key.isMissing()) {
          path.insert(0, "/" + Utils.escapeJsonPointer(key.stringValue()));
        } else if (root.getParent().getType() == BLOCK_ARRAY_ELEMENT || root.getParent().getType() == FLOW_ARRAY_ELEMENT) {
          path.insert(0, "/" + findIndex(root.getParent().getParent(), root.getParent()));
        }
      }
      root = (JsonNode)root.getParent();
    }
    this.pointer = path.toString();
    return this.pointer;
  }

  private static int findIndex(AstNode parent, AstNode child) {
    int i=0;
    for (AstNode c: parent.getChildren(FLOW_ARRAY_ELEMENT, BLOCK_ARRAY_ELEMENT)) {
      if (c == child) {
        return i;
      }
      i++;
    }
    throw new IllegalArgumentException("Cannot find child " + child.getType() + " on line " + child.getTokenLine() + " in parent " + parent.getType() + " on line " + parent.getTokenLine());
  }

  /**
   * Get the direct AST children, as JsonNode instead of ASTNode.
   */
  public final List<JsonNode> getJsonChildren() {
    return (List)getChildren();
  }

  /**
   * Get the object property named {@code fieldName}. Unlike {@link #at(String)}, it works only for object fields.
   * @param fieldName the name of the field
   * @return the matching child node, or the Missing node if not found.
   */
  public JsonNode get(String fieldName) {
    return MissingNode.MISSING;
  }

  public boolean isProperty() {
    return false;
  }

  public boolean isObject() {
    return false;
  }

  public boolean isArray() {
    return false;
  }

  public boolean isMissing() {
    return false;
  }

  public boolean isScalar() {
    return false;
  }

  public boolean isNull() {
    return false;
  }

  public boolean isSyntax() {
    return false;
  }

  /**
   * Get the property key node associated to this node. If this node is not a property node, a property value node or a
   * property key node, return the Missing node.
   * @return the associated key node, or the Missing node.
   */
  public JsonNode key() {
    AstNode sibling = getPreviousSibling();
    if (sibling != null) {
      if (sibling.getType() == KEY) {
        return this;
      } else if (sibling.getType() == VALUE) {
        sibling = sibling.getPreviousSibling();
        if (sibling.getType() == KEY) {
          return MISSING;
        } else {
          return (JsonNode)sibling;
        }
      }
    }
    return MISSING;
  }

  /**
   * Get the direct AST children matching the supplied types, as JsonNode instead of ASTNode.
   */
  public final List<JsonNode> getJsonChildren(AstNodeType... types) {
    return (List)getChildren(types);
  }

  /**
   * If this node is a property node, a property value node or a property key node, get the value node. Else return the
   * Missing node.
   * @return the associated value node, or the Missing node.
   */
  public JsonNode value() {
    AstNode sibling = getPreviousSibling();
    if (sibling != null) {
      if (sibling.getType() == VALUE) {
        return this;
      } else if (sibling.getType() == KEY) {
        sibling = getNextSibling();
        if (sibling != null && sibling.getType() == VALUE) {
          return sibling.getNextSibling() == null ? MISSING : (JsonNode) sibling.getNextSibling();
        }
      }
    }
    return MISSING;
  }

  /**
   * Get the list of property names for this node, if the node represents an object. Else returns an empty collection.
   */
  public List<String> propertyNames() {
    return Collections.emptyList();
  }

  /**
   * Get the list of property nodes for this node, if the node represents an object. Else returns an empty collection.
   * @return the list of nodes representing the values of the properties, or the empty list if this node is not an object
   */
  public Collection<JsonNode> properties() {
    return Collections.emptyList();
  }

  /**
   * Get the map of property nodes for this node, if the node represents an object. Else returns an empty map.
   * @return the map of property nodes, indexed by property name, or the empty map if this node is not an object
   */
  public Map<String, JsonNode> propertyMap() {
    return Collections.emptyMap();
  }

  /**
   * Get the map of property nodes for this node, if the node represents an object, and applies the supplied {@code mapper}
   * to each property node. Else returns an empty map.
   * @return the map of property nodes, indexed by property name, or the empty map if this node is not an object
   */
  public <T> Map<String, T> propertyMap(Function<JsonNode, T> mapper) {
    return Collections.emptyMap();
  }

  /**
   * Get the list of element nodes for this node, if the node represents an array. Else returns an empty list.
   * @return the list of elements, or the empty list if this node is not an array
   */
  public List<JsonNode> elements() {
    return Collections.emptyList();
  }

  /**
   * Verify if this node represents a JSON Schema's reference object. A reference object has a unique {@code $ref}
   * property.
   * @return {@code true} if this node is a reference object
   */
  public boolean isRef() {
    return false;
  }

  /**
   * Resolve this reference to the actual node. This only supports references to the current document.
   * If this node is not a reference, returns {@code this}.
   *
   * @return the resolved node
   */
  public JsonNode resolve() {
    return this;
  }

  /**
   * Return the value of this node, if this is a scalar node.
   */
  public String stringValue() {
    return "";
  }

  /**
   * Return the value of this node, if this is a boolean node. Else return false.
   */
  public boolean booleanValue() {
    return false;
  }

  /**
   * Try to parse this node's value as a float, if this is a scalar node. Else return {@code 0.0}.
   */
  public double floatValue() {
    return 0.0;
  }

  /**
   * Try to parse this node's value as an int, if this is a scalar node. Else return {@code 0}.
   */
  public int intValue() {
    return 0;
  }

  protected JsonNode _at(JsonPointer ptr) {
    return MISSING;
  }

  public final void decorate(AstNodeType type) {
    this.type = type;
  }
}