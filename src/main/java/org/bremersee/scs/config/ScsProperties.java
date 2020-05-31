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
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import org.springframework.util.AntPathMatcher;
import org.springframework.validation.annotation.Validated;

/**
 * The static content server properties.
 *
 * @author Christian Bremer
 */
@ConfigurationProperties(prefix = "bremersee.scs")
@RefreshScope
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Validated
@Slf4j
public class ScsProperties {

  /**
   * The classpath prefix.
   */
  static final String CLASSPATH_PREFIX = "classpath:";

  @NotNull
  private String pattern = "/**";

  @NotNull
  private String contentLocation = CLASSPATH_PREFIX + "content/";

  private String defaultIndex = "index.html";

  @NotNull
  private Map<String, String> directoryPatternIndexMap = new LinkedHashMap<>();

  /**
   * Gets content location.
   *
   * @return the content location
   */
  public String getContentLocation() {
    return Optional.of(contentLocation)
        .map(String::trim)
        .map(value -> !value.endsWith("/") ? value + "/" : value)
        .orElse(contentLocation);
  }

  /**
   * Create content root resource.
   *
   * @return the resource
   */
  public Resource createContentResource() {
    final String location = getContentLocation();
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
      path = tmp.startsWith("/") ? tmp : "/" + tmp;
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
          if (key.startsWith("[")) {
            key = key.substring(1);
          }
          if (key.endsWith("]")) {
            key = key.substring(0, key.length() - 1);
          }
          log.debug("Key {} -> {}", entry.getKey(), key);
          return Pair.of(key, entry.getValue());
        })
        .peek(pair -> log.debug("Pattern {} matches path {}? {}", pair.getFirst(), path,
            new AntPathMatcher().match(pair.getFirst(), path)))
        .filter(pair -> new AntPathMatcher().match(pair.getFirst(), path))
        .findFirst()
        .map(Pair::getSecond)
        .map(index -> {
          if (index.startsWith("/")) {
            return index.substring(1);
          }
          return index;
        });
  }

}
