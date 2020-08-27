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

import static org.springframework.web.reactive.function.server.RouterFunctions.resources;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * The web configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@EnableConfigurationProperties(ScsProperties.class)
@Slf4j
public class WebConfiguration {

  /**
   * The static content server router function.
   *
   * @param properties the properties
   * @return the router function
   */
  @RefreshScope
  @Bean
  public RouterFunction<ServerResponse> scsRouter(ScsProperties properties) {
    log.info("Creating static content router with {}", properties);
    return resources(new ScsResourceLookupFunction(properties));
  }

}
