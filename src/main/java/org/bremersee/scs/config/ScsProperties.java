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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.AntPathMatcher;

/**
 * The static content server properties.
 *
 * @author Christian Bremer
 */
@ConfigurationProperties(prefix = "bremersee.scs")
@RefreshScope
@Data
@Slf4j
public class ScsProperties {

  /**
   * The classpath prefix.
   */
  static final String CLASSPATH_PREFIX = "classpath:";

  /**
   * The default classpath location.
   */
  static final String DEFAULT_CLASSPATH_LOCATION = CLASSPATH_PREFIX + "content/";

  /**
   * The path separator.
   */
  static final String PATH_SEPARATOR = "/";

  private String pattern = "/**";

  private String contentLocation = "/opt/content/";

  private String defaultIndex = "index.html";

  private Map<String, String> directoryPatternIndexMap = new LinkedHashMap<>();

  private boolean basicAuthEnabled = false;

  private String serverAccessRole;


  /**
   * Instantiates new scs properties.
   */
  public ScsProperties() {
    super();
  }

  /**
   * Gets content location.
   *
   * @return the content location
   */
  public String getContentLocation() {
    return Optional.of(contentLocation)
        .map(String::trim)
        .map(value -> value.endsWith(PATH_SEPARATOR) ? value : value + PATH_SEPARATOR)
        .orElse(contentLocation);
  }

  /**
   * Create content root resource.
   *
   * @return the resource
   */
  public Resource createContentResource() {
    return createContentResource(getContentLocation());
  }

  /**
   * Create content root resource.
   *
   * @param location the location
   * @return the resource
   */
  static Resource createContentResource(String location) {
    return location.toLowerCase().startsWith(CLASSPATH_PREFIX)
        ? new ClassPathResource(location.substring(CLASSPATH_PREFIX.length()))
        : new FileSystemResource(location);
  }

  /**
   * Find directory index.
   *
   * @param requestPath the request path
   * @return the directory index
   */
  public Optional<String> findDirectoryIndex(String requestPath) {
    final String path;
    if (requestPath != null) {
      String tmp = requestPath.trim();
      path = tmp.startsWith(PATH_SEPARATOR) ? tmp : PATH_SEPARATOR + tmp;
    } else {
      path = "/";
    }
    log.debug("Looking for index of {}", path);
    Map<String, String> map = getDirectoryPatternIndexMap();
    if (!(map.containsKey("/**") || map.containsKey("[/**]"))) {
      map.put("/**", defaultIndex);
    }
    return map.entrySet().stream()
        .map(entry -> {
          String key = entry.getKey();
          log.debug("Key {} -> {}", entry.getKey(), key);
          return new Pair(key, entry.getValue());
        })
        .peek(pair -> log.debug("Pattern {} matches path {}? {}", pair.first(), path,
            new AntPathMatcher().match(pair.first(), path)))
        .filter(pair -> new AntPathMatcher().match(pair.first(), path))
        .findFirst()
        .map(Pair::second)
        .map(index -> {
          String tmp = index;
          while (tmp.startsWith(PATH_SEPARATOR)) {
            tmp = tmp.substring(1);
          }
          return tmp;
        });
  }

  private record Pair(String first, String second) {

  }

}
