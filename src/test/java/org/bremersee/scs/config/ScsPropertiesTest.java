/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bremersee.scs.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * The static content server properties test.
 *
 * @author Christian Bremer
 */
class ScsPropertiesTest {

  /**
   * Gets pattern.
   */
  @Test
  void getPattern() {
    String value = "/demo/**";
    ScsProperties actual = new ScsProperties();
    actual.setPattern(value);
    assertEquals(value, actual.getPattern());

    ScsProperties expected = new ScsProperties();
    expected.setPattern(value);

    assertEquals(actual, actual);
    assertEquals(actual.hashCode(), actual.hashCode());
    assertEquals(expected, actual);
    assertEquals(expected.hashCode(), actual.hashCode());
    //noinspection SimplifiableJUnitAssertion
    assertTrue(expected.equals(actual));
    assertNotEquals(expected, null);
    assertNotEquals(expected, new Object());

    assertTrue(actual.toString().contains(value));
  }

  /**
   * Gets content location.
   */
  @Test
  void getContentLocation() {
    String value = "/var/lib/scs/content";
    ScsProperties actual = new ScsProperties();
    actual.setContentLocation(value);
    assertEquals(value + "/", actual.getContentLocation());

    value = "/var/lib/scs/content/";
    actual.setContentLocation(value);
    assertEquals(value, actual.getContentLocation());

    ScsProperties expected = new ScsProperties();
    expected.setContentLocation(value);

    assertEquals(actual, actual);
    assertEquals(expected, actual);
    assertEquals(expected.hashCode(), actual.hashCode());
    //noinspection SimplifiableJUnitAssertion
    assertTrue(expected.equals(actual));
    assertNotEquals(expected, null);
    assertNotEquals(expected, new Object());

    assertTrue(actual.toString().contains(value));
  }

  /**
   * Gets default index.
   */
  @Test
  void getDefaultIndex() {
    String value = "home.html";
    ScsProperties actual = new ScsProperties();
    actual.setDefaultIndex(value);
    assertEquals(value, actual.getDefaultIndex());

    ScsProperties expected = new ScsProperties();
    expected.setDefaultIndex(value);

    assertEquals(actual, actual);
    assertEquals(expected, actual);
    assertEquals(expected.hashCode(), actual.hashCode());
    //noinspection SimplifiableJUnitAssertion
    assertTrue(expected.equals(actual));
    assertNotEquals(expected, null);
    assertNotEquals(expected, new Object());

    assertTrue(actual.toString().contains(value));
  }

  /**
   * Gets directory pattern index map.
   */
  @Test
  void getDirectoryPatternIndexMap() {
    Map<String, String> value = new LinkedHashMap<>();
    value.put("/somewhere/**", "some.html");
    ScsProperties actual = new ScsProperties();
    actual.setDirectoryPatternIndexMap(value);
    assertEquals(value, actual.getDirectoryPatternIndexMap());

    ScsProperties expected = new ScsProperties();
    expected.setDirectoryPatternIndexMap(value);

    assertEquals(actual, actual);
    assertEquals(expected, actual);
    assertEquals(expected.hashCode(), actual.hashCode());
    //noinspection SimplifiableJUnitAssertion
    assertTrue(expected.equals(actual));
    assertNotEquals(expected, null);
    assertNotEquals(expected, new Object());

    assertTrue(actual.toString().contains(value.toString()));
  }

  /**
   * Create content resource.
   */
  @Test
  void createContentResource() {
    ScsProperties properties = new ScsProperties();
    properties.setContentLocation(ScsProperties.CLASSPATH_PREFIX + "content/");
    Resource resource = properties.createContentResource();
    assertNotNull(resource);
    assertTrue(resource instanceof ClassPathResource);

    properties.setContentLocation("/tmp");
    resource = properties.createContentResource();
    assertNotNull(resource);
    assertTrue(resource instanceof FileSystemResource);
  }

  /**
   * Find directory index.
   */
  @Test
  void findDirectoryIndex() {
    Map<String, String> value = new LinkedHashMap<>();
    value.put("/somewhere/**", "some.html");
    ScsProperties properties = new ScsProperties();
    properties.setDirectoryPatternIndexMap(value);
    properties.setDefaultIndex("/home.html");

    Optional<String> index = properties
        .findDirectoryIndex("/somewhere/over/the/rainbow");
    assertTrue(index.isPresent());
    assertEquals("some.html", index.get());

    index = properties
        .findDirectoryIndex("/nowhere/man");
    assertTrue(index.isPresent());
    assertEquals("home.html", index.get());

    index = properties.findDirectoryIndex(null);
    assertTrue(index.isPresent());
    assertEquals("home.html", index.get());
  }

}