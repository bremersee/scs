/*
 * Copyright 2026 the original author or authors.
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

import static org.springframework.util.ObjectUtils.isEmpty;

import org.bremersee.spring.boot.autoconfigure.security.authentication.AuthenticationProperties;
import org.bremersee.spring.security.ldaptive.authentication.ReactiveLdaptiveAuthenticationManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.reactive.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.AuthorizeExchangeSpec.Access;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.config.web.server.ServerHttpSecurity.FormLoginSpec;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.util.StringUtils;

/**
 * The web security configuration.
 *
 * @author Christian Bremer
 */
@EnableWebFluxSecurity
@EnableConfigurationProperties({
    AuthenticationProperties.class, ScsProperties.class
})
@Configuration
class WebSecurityConfiguration {

  private final ScsProperties scsProperties;

  private final AuthenticationProperties authenticationProperties;

  private final ReactiveLdaptiveAuthenticationManager ldaptiveAuthenticationManager;

  private final ReactiveUserDetailsService userDetailsService;

  /**
   * Instantiates a new web security configuration.
   *
   * @param scsProperties the scs properties
   * @param authenticationProperties the authentication properties
   * @param ldaptiveAuthenticationManager the ldaptive authentication manager
   * @param userDetailsService the user details service
   */
  WebSecurityConfiguration(
      ScsProperties scsProperties,
      AuthenticationProperties authenticationProperties,
      ObjectProvider<ReactiveLdaptiveAuthenticationManager> ldaptiveAuthenticationManager,
      ObjectProvider<ReactiveUserDetailsService> userDetailsService) {

    this.scsProperties = scsProperties;
    this.authenticationProperties = authenticationProperties;
    this.ldaptiveAuthenticationManager = ldaptiveAuthenticationManager.getIfAvailable();
    this.userDetailsService = userDetailsService.getIfAvailable();
  }

  /**
   * Creates a security web filter chain.
   *
   * @param http the http
   * @return the security filter chain
   */
  @Bean
  SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
    String[] actuatorReadRoles = authenticationProperties.getActuator().getReadRoles()
        .toArray(new String[0]);
    String[] actuatorWriteRoles = authenticationProperties.getActuator().getWriteRoles()
        .toArray(new String[0]);
    http
        .authorizeExchange(authorize -> {
          Access access = authorize
              .pathMatchers(HttpMethod.OPTIONS, "/**")
              .permitAll()

              .matchers(
                  EndpointRequest.to(InfoEndpoint.class),
                  EndpointRequest.to(HealthEndpoint.class))
              .permitAll()

              .matchers(new AndServerWebExchangeMatcher(
                  EndpointRequest.toAnyEndpoint(),
                  new PathPatternParserServerWebExchangeMatcher("/**", HttpMethod.GET)))
              .hasAnyAuthority(actuatorReadRoles)

              .matchers(EndpointRequest.toAnyEndpoint())
              .hasAnyAuthority(actuatorWriteRoles)

              .anyExchange();

          if (scsProperties.isBasicAuthEnabled()) {
            if (StringUtils.hasLength(scsProperties.getServerAccessRole())) {
              access.hasAuthority(scsProperties.getServerAccessRole());
            } else {
              access.authenticated();
            }
          } else {
            access.permitAll();
          }
        })

        .csrf(CsrfSpec::disable)

        .headers(headerSpec -> headerSpec
            .frameOptions(fos -> fos.mode(Mode.SAMEORIGIN)))

        .formLogin(FormLoginSpec::disable);

    if (!isEmpty(ldaptiveAuthenticationManager)) {
      http.httpBasic(customizer -> customizer
          .authenticationManager(ldaptiveAuthenticationManager));
    } else if (!isEmpty(userDetailsService)) {
      var authenticationManager = new UserDetailsRepositoryReactiveAuthenticationManager(
          userDetailsService);
      http.httpBasic(customizer -> customizer
          .authenticationManager(authenticationManager));
    }

    return http.build();
  }

}
