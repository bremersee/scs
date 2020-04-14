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

import java.util.stream.Collectors;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;

/**
 * The security configuration.
 */
@Configuration
@EnableConfigurationProperties(AuthenticationProperties.class)
public class SecurityConfiguration {

  private AuthenticationProperties properties;

  /**
   * Instantiates a new security configuration.
   *
   * @param properties the properties
   */
  public SecurityConfiguration(AuthenticationProperties properties) {
    this.properties = properties;
  }

  /**
   * User details service map reactive user details service.
   *
   * @return the map reactive user details service
   */
  @Bean
  public MapReactiveUserDetailsService userDetailsService() {
    final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    return new MapReactiveUserDetailsService(properties.buildUsers().stream()
        .map(simpleUser -> User.builder()
            .username(simpleUser.getName())
            .password(simpleUser.getPassword())
            .passwordEncoder(encoder::encode)
            .authorities(simpleUser.getAuthorities().toArray(new String[0]))
            .build())
        .collect(Collectors.toList()));
  }

  /**
   * Builds content server filter chain.
   *
   * @param http the http
   * @return the security web filter chain
   */
  @Bean
  @Order(51)
  public SecurityWebFilterChain contentServerFilterChain(ServerHttpSecurity http) {

    return http
        .securityMatcher(new NegatedServerWebExchangeMatcher(EndpointRequest.toAnyEndpoint()))
        .csrf().disable()
        .authorizeExchange()
        .pathMatchers("/**")
        .permitAll()
        .and()
        .httpBasic()
        .and()
        .formLogin().disable()
        .build();
  }

  /**
   * Builds the actuator filter chain.
   *
   * @param http the http security configuration object
   * @return the security web filter chain
   */
  @Bean
  @Order(52)
  public SecurityWebFilterChain actuatorFilterChain(ServerHttpSecurity http) {

    return http
        .securityMatcher(EndpointRequest.toAnyEndpoint())
        .csrf().disable()
        .authorizeExchange()
        .pathMatchers(HttpMethod.OPTIONS).permitAll()
        .matchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
        .matchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
        .anyExchange().hasAuthority(properties.getActuatorRoleName())
        .and()
        .httpBasic()
        .and()
        .formLogin().disable()
        .build();
  }

}
