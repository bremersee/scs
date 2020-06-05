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

import static org.bremersee.scs.config.ScsProperties.DEFAULT_CLASSPATH_LOCATION;
import static org.bremersee.scs.config.ScsProperties.createContentResource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.server.PathContainer;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

/**
 * The static content resource lookup function.
 */
@Validated
@Slf4j
public class ScsResourceLookupFunction implements Function<ServerRequest, Mono<Resource>> {

  private static final PathPatternParser PATTERN_PARSER = new PathPatternParser();

  private final PathPattern pattern;

  private final Resource location;

  private final ScsProperties properties;

  /**
   * Instantiates a new resource lookup function.
   *
   * @param properties the properties
   */
  public ScsResourceLookupFunction(@NotNull @Valid ScsProperties properties) {
    this.properties = properties;
    this.pattern = PATTERN_PARSER.parse(properties.getPattern());
    if (!this.properties.getContentLocation().toLowerCase()
        .startsWith(ScsProperties.CLASSPATH_PREFIX)) {
      File dir = new File(this.properties.getContentLocation());
      if (!dir.exists() || !dir.isDirectory()) {
        log.warn("Content location {} does not exist, using fallback location {}",
            this.properties.getContentLocation(), DEFAULT_CLASSPATH_LOCATION);
        this.location = createContentResource(DEFAULT_CLASSPATH_LOCATION);
      } else {
        this.location = properties.createContentResource();
      }
    } else {
      this.location = properties.createContentResource();
    }
  }

  @SuppressWarnings("BlockingMethodInNonBlockingContext")
  @Override
  public Mono<Resource> apply(ServerRequest request) {
    PathContainer pathContainer = request.pathContainer();
    log.debug("Looking for static content of request path {}", pathContainer.value());
    if (!this.pattern.matches(pathContainer)) {
      log.debug("Pattern {} does not match request path, return no static content resource.",
          properties.getPattern());
      return Mono.empty();
    }
    pathContainer = this.pattern.extractPathWithinPattern(pathContainer);
    String path = pathContainer.value();
    log.debug("Extracted path is {}", path);
    path = processPath(pathContainer.value());
    log.debug("Processed path is {}", path);
    if (path.contains("%")) {
      path = StringUtils.uriDecode(path, StandardCharsets.UTF_8);
      log.debug("Url decoded path is {}", path);
    }
    if (!StringUtils.hasLength(path)) {
      log.debug("Path is empty, trying to find an index file.");
      path = findDirectoryIndex(path);
      if (StringUtils.isEmpty(path)) {
        log.debug("No index file was found, return no static content resource.");
        return Mono.empty();
      }
      log.debug("An index file was found, path is {}", path);
    }

    try {
      Resource resource = this.location.createRelative(path);
      boolean exists = resource.exists();
      boolean isReadable = resource.isReadable();
      log.debug("Resource exists? {}, resource is readable? {}", exists, isReadable);
      if (exists && !isReadable) {
        String pathWithIndex = findDirectoryIndex(path);
        if (path.equals(pathWithIndex)) {
          log.debug("No index file was found, return no static content resource.");
          return Mono.empty();
        }
        path = pathWithIndex;
        resource = this.location.createRelative(path);
        isReadable = resource.isReadable();
      }
      if (exists && isReadable) {
        log.debug("Returning static content resource {}", path);
        return Mono.just(resource);
      } else {
        log.debug("Resource ('{}') does not exists.", path);
        return Mono.empty();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private String processPath(String path) {
    boolean slash = false;
    for (int i = 0; i < path.length(); i++) {
      if (path.charAt(i) == '/') {
        slash = true;
      } else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
        if (i == 0 || (i == 1 && slash)) {
          return path;
        }
        path = slash ? "/" + path.substring(i) : path.substring(i);
        return path;
      }
    }
    return (slash ? "/" : "");
  }

  private String findDirectoryIndex(String path) {
    return properties.findDirectoryIndex(path)
        .map(index -> {
          if (StringUtils.hasLength(path)) {
            return path + "/" + index;
          }
          return index;
        })
        .orElse(path);
  }

}
