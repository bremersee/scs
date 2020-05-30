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

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.resources;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * The web configuration.
 */
@Configuration
@Slf4j
public class WebConfiguration {

  private static final String CLASSPATH_RESOURCE = "classpath:";

  /**
   * Root router.
   *
   * @param contentLocation the content location
   * @param rootResource the root resource
   * @return the router function
   */
  @RefreshScope
  @Bean
  public RouterFunction<ServerResponse> rootRouter(
      @Value("${bremersee.scs.content-location:/opt/content/}") String contentLocation,
      @Value("${bremersee.scs.root-resource:index.html}") String rootResource) {

    return route(
        GET("/"),
        request -> ok().bodyValue(resource(contentLocation(contentLocation) + rootResource)));
  }

  /**
   * Content router.
   *
   * @param contentLocation the content location
   * @return the router function
   */
  @RefreshScope
  @Bean
  public RouterFunction<ServerResponse> scsRouter(
      @Value("${bremersee.scs.content-location:/opt/content/}") String contentLocation) {


    return resources(request -> {
      log.info("uri = {}", request.uri());
      log.info("headers = {}", request.headers().asHttpHeaders());
      return Mono.just(resource(contentLocation(contentLocation)));

    });
    /*
    return RouterFunctions.route().GET("/**"), request -> {
      log.info("uri = {}", request.uri());
      log.info("headers = {}", request.headers().asHttpHeaders());
      return ServerResponse.ok().body(BodyInserters.fromResource(resource(contentLocation(contentLocation))));
    };
     */
    // return resources("/**", resource(contentLocation(contentLocation)));
  }

  private static String contentLocation(String contentLocation) {
    return Optional.ofNullable(contentLocation)
        .map(String::trim)
        .map(value -> !value.endsWith("/") ? value + "/" : value)
        .orElse("/opt/content/");
  }

  private static Resource resource(String location) {
    Assert.notNull(location, "Resource location must not be null.");
    return location.toLowerCase().startsWith(CLASSPATH_RESOURCE)
        ? new ClassPathResource(location.substring(CLASSPATH_RESOURCE.length()))
        : new FileSystemResource(location);
  }

}
