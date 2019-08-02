/*
 * Sonar SSLR :: YAML Parser
 * Copyright (C) 2018-2018 Societe Generale
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
package org.sonar.sslr.yaml.grammar.experimental;

import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.TokenType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.typed.Choice;
import org.sonar.sslr.yaml.grammar.typed.GrammarRule;
import org.sonar.sslr.yaml.grammar.typed.Key;
import org.sonar.sslr.yaml.grammar.typed.Pattern;
import org.sonar.sslr.yaml.grammar.typed.Resolvable;
import org.sonar.sslr.yaml.grammar.typed.ResolveException;

import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.FALSE;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.FLOAT;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.INTEGER;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.NULL;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.STRING;
import static org.sonar.sslr.yaml.snakeyaml.parser.Tokens.TRUE;

public class AstProxy implements InvocationHandler {
  private final Map<String, Object> values = new HashMap<>();
  private final JsonNode node;

  private AstProxy(JsonNode node) {
    this.node = node;
    this.values.put("getKey", node.getType());
    this.values.put("getNode", node);

  }

  @Override
  public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
    if (method.getDeclaringClass().equals(Object.class)) {
      return method.invoke(node, objects);
    }

    Object value = values.get(method.getName());
    if (value == null) {
      throw new IllegalAccessException("Method " + method + " is not a valid proxy");
    }

    return value;
  }

  private String getTargetPropertyName(Method method) {
    Key property = method.getAnnotation(Key.class);
    String propertyName;
    if (property == null || property.value().isEmpty()) {
      propertyName = method.getName();
    } else {
      propertyName = property.value();
    }
    return propertyName;
  }

  private JsonNode getTargetNode(Method method) {
    return node.at("/" + getTargetPropertyName(method));
  }

  private Object findValue(Method method) {
    Class<?> type = method.getReturnType();
    Pattern pattern = method.getAnnotation(Pattern.class);
    Choice choice = method.getAnnotation(Choice.class);
    if (type.isAssignableFrom(Map.class) && pattern != null) {
      // Special case: Map<String, T> someProperty(), where someProperty() is an accessor to patterned fields
      return proxyMap(pattern.value(), node, method);
    }
    if (isCollectionType(type)) {
      return proxyCollectionValue(method);
    }
    if (isForbiddenCollectionType(type)) {
      throw new IllegalArgumentException("This return type canot be proxied");
    }
    String propertyName = getTargetPropertyName(method);
    JsonNode child = node.at("/" + propertyName);
    if (child.isMissing() || child.getToken().getType() == NULL) {
      return null;
    }
    if (type.isAssignableFrom(Object.class) && choice != null) {
      return proxyPolymorphic(child, method);
    }
    Object proxy = proxySimpleType(type, child);
    if (proxy == null) {
      throw new IllegalArgumentException("No matching type for field " + method.getName() + " of type " + child.getType());
    } else {
      return proxy;
    }
  }

  private Object proxyPolymorphic(JsonNode child, Method method) {
    Choice property = method.getAnnotation(Choice.class);

    for (Class<?> type : property.classes()) {
      Object proxy = proxySimpleType(type, child);
      if (proxy != null) {
        return proxy;
      }
    }
    throw new IllegalArgumentException("No matching type for field " + method.getName() + " of type " + child.getType());
  }

  private Object proxyCollectionValue(Method method) {
    Class<?> returnClass = method.getReturnType();
    JsonNode child = getTargetNode(method);
    if (returnClass.isAssignableFrom(Map.class)) {
      return proxyMap(".*", child, method);
    } else if (returnClass.isAssignableFrom(List.class)) {
      return proxyList(child, method);
    }
    throw new IllegalArgumentException("Cannot proxy method " + method);
  }

  private Object proxyList(JsonNode child, Method method) {
    if (child.isMissing() || child.getToken().getType() == NULL) {
      return Collections.emptyList();
    }
    Choice property = method.getAnnotation(Choice.class);
    ParameterizedType paramType = (ParameterizedType) method.getGenericReturnType();
    Type valueType = paramType.getActualTypeArguments()[0];
    if (property != null && property.classes().length > 0 && valueType == Object.class) {
      return new ListProxy(child, property.classes());
    } else {
      return new ListProxy(child, (Class) valueType);
    }
  }

  private Object proxyMap(String pattern, JsonNode node, Method method) {
    if (node.isMissing() || node.getToken().getType() == NULL) {
      return Collections.emptyMap();
    }
    Choice property = method.getAnnotation(Choice.class);
    ParameterizedType paramType = (ParameterizedType) method.getGenericReturnType();
    Type valueType = paramType.getActualTypeArguments()[1];
    if (property != null && property.classes().length > 0 && valueType == Object.class) {
      return new MapProxy(node, pattern, property.classes());
    } else if (valueType instanceof Class){
      return new MapProxy(node, pattern, (Class) valueType);
    } else {
      // Map of collection
      return Collections.emptyMap();
    }
  }

  private static boolean isCollectionType(Class<?> type) {
    if (type == Object.class) {
      return false;
    }
    return type.isAssignableFrom(Map.class) || type.isAssignableFrom(List.class);
  }

  private static boolean isForbiddenCollectionType(Class<?> type) {
    if (type == Object.class) {
      return false;
    }
    return type.isAssignableFrom(Set.class) || type.isAssignableFrom(Queue.class);
  }

  private static boolean matches(Class<?> rule, AstNodeType type) {
    if (!GrammarRule.class.isAssignableFrom(rule)) {
      return false;
    }

    try {
      Field field = rule.getField("__TYPE");
      field.setAccessible(true);
      Object result = field.get(null);
      if (result.getClass().isArray()) {
        AstNodeType[] types = (AstNodeType[]) result;
        return Arrays.asList(types).contains(type);
      } else {
        return type.equals(result) || Resolvable.class.isAssignableFrom(rule) && type.toString().equals("REF");
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new ResolveException("Class " + rule + " does not define a AstNodeType __TYPE public field.");
    }
  }

  public static <T> T forNode(Class<? extends T> type, JsonNode node) {
    if (type.isAssignableFrom(JsonNode.class)) {
      return (T) node;
    }
    Object o = proxySimpleType(type, node); // TODO - in case of collection of multiple types, we should not be continuing here if type is indeed a simple type!
    if (o == null) {
      o = forAnnotatedNode(type, node);
    }
    return (T) o;
  }

  private static Object proxySimpleType(Class<?> type, JsonNode child) {
    TokenType tokenType = child.getToken().getType();
    if (matches(type, child.getType())) {
      return AstProxy.forAnnotatedNode(type, child);
    } else if (type.isAssignableFrom(JsonNode.class)) {
      return child;
    } else if (type.isAssignableFrom(Boolean.class) && (tokenType == TRUE || tokenType == FALSE)) {
      return tokenType == TRUE;
    } else if (type.isEnum() && tokenType == STRING) {
      return Enum.valueOf((Class<Enum>)type, child.getTokenValue());
    } else if (type.isAssignableFrom(String.class) && tokenType == STRING) {
      return child.getTokenValue();
    } else if (type.isAssignableFrom(BigDecimal.class) && (tokenType == INTEGER || tokenType == FLOAT)) {
      return BigDecimal.valueOf(Double.parseDouble(child.getTokenValue()));
    } else if ((type.isAssignableFrom(Integer.class) || type.isAssignableFrom(int.class)) && tokenType == INTEGER) {
      return Integer.parseInt(child.getTokenValue());
    } else if ((type.isAssignableFrom(Float.class) || type.isAssignableFrom(float.class)) && tokenType == FLOAT) {
      return Float.parseFloat(child.getTokenValue());
    } else {
      return null;
    }
  }

  private static <T> T forAnnotatedNode(Class<? extends T> type, JsonNode node) {
    AstProxy proxy = new AstProxy(node);
    if(Resolvable.class.isAssignableFrom(type)) {
      JsonNode ref = node.at("/$ref");
      proxy.values.put("$ref", ref.getTokenValue());
      proxy.values.put("isRef", node.isRef());
      if (node.isRef()) {
        JsonNode resolve = node.resolve();
        if (resolve.isMissing()) {
          throw new ResolveException("Cannot resolve reference " + ref.getTokenValue());
        }
        proxy.values.put("resolve", forNode(type, resolve));
      } else {
        proxy.values.put("resolve", proxy);
      }
    }
    for (Method method : type.getMethods()) {
      if (!method.getDeclaringClass().equals(Object.class) && !method.getDeclaringClass().equals(Resolvable.class)) {
        Object value = proxy.findValue(method);
        proxy.values.put(method.getName(), value);
      }
    }
    return (T) Proxy.newProxyInstance(AstProxy.class.getClassLoader(), new Class[] {type}, proxy);
  }

  private static class ListProxy extends ArrayList {
    private ListProxy(JsonNode node, Class<?>... valueType) {
      if (!node.isArray()) {
        throw new IllegalArgumentException("Node " + node + " is not an array");
      }
      node.elements().forEach(n -> super.add(convertTo(n, valueType)));
    }

    public Object set(int var1, Object var2) {
      throw new UnsupportedOperationException();
    }

    public void add(int var1, Object var2) {
      throw new UnsupportedOperationException();
    }

    public Object remove(int var1) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection collection) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection collection) {
      throw new UnsupportedOperationException();
    }
  }

  private static Object convertTo(JsonNode node, Class<?>... valueType) {
    for (Class<?> type : valueType) {
      Object o = AstProxy.forNode(type, node);
      if (o != null) {
        return o;
      }
    }
    throw new IllegalArgumentException("No matching type for node " + node.getType() + " on line " + node.getTokenLine() + " among " + Arrays.toString(valueType));

  }

  private static class MapProxy extends HashMap<String, Object> {
    private MapProxy(JsonNode node, String template, Class<?>... valueType) {
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(template);
      for (Entry<String, JsonNode> entry : node.propertyMap().entrySet()) {
        if (pattern.matcher(entry.getKey()).matches()) {
          super.put(entry.getKey(), AstProxy.convertTo(entry.getValue(), valueType));
        }
      }
    }

    @Override
    public Object put(String var1, Object var2) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object var1) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ?> var1) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }
  }
}
