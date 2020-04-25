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

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.security.authentication.AuthenticationProperties;
import org.bremersee.security.authentication.PasswordFlowReactiveAuthenticationManager;
import org.bremersee.security.authentication.RoleBasedAuthorizationManager;
import org.bremersee.security.authentication.RoleOrIpBasedAuthorizationManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.Assert;

/**
 * The security configuration.
 */
@ConditionalOnWebApplication
@Configuration
@EnableWebFluxSecurity
@Slf4j
public class SecurityConfiguration {

  /**
   * The jwt login.
   */
  @ConditionalOnWebApplication
  @ConditionalOnProperty(
      prefix = "bremersee.security.authentication",
      name = "enable-jwt-support",
      havingValue = "true")
  @Configuration
  @EnableConfigurationProperties(AuthenticationProperties.class)
  @Slf4j
  static class JwtLogin {

    private AuthenticationProperties properties;

    private ProtectionMode protectionMode;

    private PasswordFlowReactiveAuthenticationManager passwordFlowAuthenticationManager;

    /**
     * Instantiates a new jwt login.
     *
     * @param properties the authentication properties
     * @param protectionMode the protection mode
     * @param passwordFlowAuthenticationManager the password flow authentication manager
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public JwtLogin(
        AuthenticationProperties properties,
        @Value("${bremersee.scs.protection:none}") ProtectionMode protectionMode,
        PasswordFlowReactiveAuthenticationManager passwordFlowAuthenticationManager) {
      this.passwordFlowAuthenticationManager = passwordFlowAuthenticationManager;
      this.protectionMode = protectionMode != null ? protectionMode : ProtectionMode.NONE;
      this.properties = properties;
    }

    /**
     * User details service.
     *
     * @return the user details service
     */
    @ConditionalOnProperty(name = "bremersee.scs.protection", havingValue = "in_memory")
    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
      return new MapReactiveUserDetailsService(properties.buildBasicAuthUserDetails());
    }

    /**
     * Builds the content filter chain. If the protection mode is turned on, the content will be
     * protected with basic auth.
     *
     * @param http the http
     * @return the security web filter chain
     */
    @Bean
    @Order(51)
    @SuppressWarnings("DuplicatedCode")
    public SecurityWebFilterChain contentFilterChain(ServerHttpSecurity http) {

      log.info("Creating content filter chain with protection mode {}.", protectionMode);
      ServerHttpSecurity tmpHttp = http;
      ServerHttpSecurity.AuthorizeExchangeSpec spec = tmpHttp
          .securityMatcher(new NegatedServerWebExchangeMatcher(EndpointRequest.toAnyEndpoint()))
          .authorizeExchange()
          .pathMatchers(HttpMethod.OPTIONS).permitAll();
      if (protectionMode == ProtectionMode.NONE) {
        tmpHttp = spec
            .anyExchange().permitAll()
            .and();
      } else {
        final List<String> roles = properties.getApplication().getUserRoles();
        final boolean hasRoles = roles != null && !roles.isEmpty();
        if (hasRoles) {
          tmpHttp = spec
              .anyExchange().access(new RoleOrIpBasedAuthorizationManager(
                  roles,
                  properties.getRolePrefix(),
                  properties.getApplication().getIpAddresses()))
              .and();
        } else {
          tmpHttp = spec
              .anyExchange().authenticated()
              .and();
        }
        if (protectionMode == ProtectionMode.PASSWORD_FLOW) {
          tmpHttp = tmpHttp.authenticationManager(passwordFlowAuthenticationManager);
        }
        tmpHttp = tmpHttp
            .httpBasic()
            .and()
            .formLogin().disable();
      }
      return tmpHttp
          .csrf().disable()
          .build();
    }

    /**
     * Builds actuator filter chain wth JWT.
     *
     * @param http the http
     * @return the security web filter chain
     */
    @Bean
    @Order(52)
    @SuppressWarnings("DuplicatedCode")
    public SecurityWebFilterChain actuatorFilterChain(ServerHttpSecurity http) {

      log.info("Creating actuator filter chain with JWT.");
      return http
          .securityMatcher(EndpointRequest.toAnyEndpoint())
          .authorizeExchange()
          .pathMatchers(HttpMethod.OPTIONS).permitAll()
          .matchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
          .matchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
          .matchers(new AndServerWebExchangeMatcher(
              EndpointRequest.toAnyEndpoint(),
              ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/**")))
          .access(new RoleOrIpBasedAuthorizationManager(
              properties.getActuator().getRoles(),
              properties.getRolePrefix(),
              properties.getActuator().getIpAddresses()))
          .matchers(EndpointRequest.toAnyEndpoint())
          .access(new RoleBasedAuthorizationManager(
              properties.getActuator().getAdminRoles(),
              properties.getRolePrefix()))
          .anyExchange().denyAll()
          .and()
          .httpBasic()
          .authenticationManager(passwordFlowAuthenticationManager)
          .and()
          .formLogin().disable()
          .csrf().disable()
          .build();
    }
  }

  /**
   * The in-memory login.
   */
  @ConditionalOnWebApplication
  @ConditionalOnProperty(
      prefix = "bremersee.security.authentication",
      name = "enable-jwt-support",
      havingValue = "false", matchIfMissing = true)
  @Configuration
  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class InMemoryLogin {

    private AuthenticationProperties properties;

    private ProtectionMode protectionMode;

    /**
     * Instantiates a new in-memory login.
     *
     * @param properties the authentication properties
     * @param protectionMode the protection mode
     */
    public InMemoryLogin(
        AuthenticationProperties properties,
        @Value("${bremersee.scs.protection:none}") ProtectionMode protectionMode) {
      this.properties = properties;
      this.protectionMode = protectionMode != null ? protectionMode : ProtectionMode.NONE;
      Assert.isTrue(
          this.protectionMode != ProtectionMode.PASSWORD_FLOW,
          "JWT is disabled. So protection mode must not be password_flow.");
    }

    /**
     * User details service.
     *
     * @return the user details service
     */
    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
      return new MapReactiveUserDetailsService(properties.buildBasicAuthUserDetails());
    }

    /**
     * Builds the content filter chain. If protection mode {@link ProtectionMode#IN_MEMORY}, the
     * content will be protected with basic auth and accessible to the in-memory users.
     *
     * @param http the http
     * @return the security web filter chain
     */
    @Bean
    @Order(51)
    @SuppressWarnings("DuplicatedCode")
    public SecurityWebFilterChain contentFilterChain(ServerHttpSecurity http) {

      log.info("Creating content filter chain with protection mode {}.", protectionMode);
      ServerHttpSecurity tmpHttp = http;
      ServerHttpSecurity.AuthorizeExchangeSpec spec = tmpHttp
          .securityMatcher(new NegatedServerWebExchangeMatcher(EndpointRequest.toAnyEndpoint()))
          .authorizeExchange()
          .pathMatchers(HttpMethod.OPTIONS).permitAll();
      if (protectionMode == ProtectionMode.IN_MEMORY) {
        final List<String> roles = properties.getApplication().getUserRoles();
        final boolean hasRoles = roles != null && !roles.isEmpty();
        if (hasRoles) {
          tmpHttp = spec
              .anyExchange().access(new RoleOrIpBasedAuthorizationManager(
                  roles,
                  properties.getRolePrefix(),
                  properties.getApplication().getIpAddresses()))
              .and();
        } else {
          tmpHttp = spec
              .anyExchange().authenticated()
              .and();
        }
        tmpHttp = tmpHttp
            .httpBasic()
            .and()
            .formLogin().disable();
      } else {
        tmpHttp = spec
            .anyExchange().permitAll()
            .and();
      }
      return tmpHttp
          .csrf().disable()
          .build();
    }

    /**
     * Builds the actuator filter chain with in-memory users and basic auth.
     *
     * @param http the http
     * @return the security web filter chain
     */
    @Bean
    @Order(52)
    @SuppressWarnings("DuplicatedCode")
    public SecurityWebFilterChain actuatorFilterChain(ServerHttpSecurity http) {

      log.info("Creating actuator filter chain with in-memory user authentication.");
      return http
          .securityMatcher(EndpointRequest.toAnyEndpoint())
          .authorizeExchange()
          .pathMatchers(HttpMethod.OPTIONS).permitAll()
          .matchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
          .matchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
          .matchers(new AndServerWebExchangeMatcher(
              EndpointRequest.toAnyEndpoint(),
              ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/**")))
          .access(new RoleOrIpBasedAuthorizationManager(
              properties.getActuator().getRoles(),
              properties.getRolePrefix(),
              properties.getActuator().getIpAddresses()))
          .matchers(EndpointRequest.toAnyEndpoint())
          .access(new RoleBasedAuthorizationManager(
              properties.getActuator().getAdminRoles(),
              properties.getRolePrefix()))
          .anyExchange().denyAll()
          .and()
          .httpBasic()
          .and()
          .formLogin().disable()
          .csrf().disable()
          .build();
    }
  }

}
