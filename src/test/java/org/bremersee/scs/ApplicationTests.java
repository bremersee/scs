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

package org.bremersee.scs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * The application tests.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "bremersee.scs.pattern=/**",
    "bremersee.scs.content-location=classpath:content/",
    "bremersee.scs.directory-pattern-index-map.[/dir/**]:dir.html",
    "bremersee.scs.directory-pattern-index-map.[/**]:index.html"
})
@ActiveProfiles({"in-memory"})
class ApplicationTests {

  /**
   * The web test client.
   */
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  WebTestClient webTestClient;

  /**
   * Gets root.
   */
  @Test
  void getRoot() {
    webTestClient
        .get()
        .uri("/")
        .accept(MediaType.ALL)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .value(content -> assertTrue(content.contains("Static content server is running.")));
  }

  /**
   * Gets root with hash.
   */
  @Test
  void getRootWithHash() {
    webTestClient
        .get()
        .uri("/#state")
        .accept(MediaType.ALL)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .value(content -> assertTrue(content.contains("Static content server is running.")));
  }

  /**
   * Gets example content.
   */
  @Test
  void getExampleContent() {
    webTestClient
        .get()
        .uri("/example.html?foo=bar")
        .accept(MediaType.ALL)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .value(content -> assertTrue(content.contains("Example content.")));
  }

  /**
   * Gets dir content.
   */
  @Test
  void getDirContent() {
    webTestClient
        .get()
        .uri("/dir/dir.html")
        .accept(MediaType.ALL)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .value(content -> assertTrue(content.contains("Dir content.")));
  }

  /**
   * Gets dir content without file.
   */
  @Test
  void getDirContentWithoutFile() {
    webTestClient
        .get()
        .uri("/dir")
        .accept(MediaType.ALL)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .value(content -> assertTrue(content.contains("Dir content.")));
  }

  /**
   * Gets health.
   */
  @Test
  void getHealth() {
    webTestClient
        .get()
        .uri("/actuator/health")
        .accept(MediaType.ALL)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .value(content -> assertFalse(content.isBlank()));
  }

  /**
   * Gets metrics.
   */
  @WithMockUser(
      username = "actuator",
      password = "actuator",
      authorities = {"ROLE_ACTUATOR"})
  @Test
  void getMetrics() {
    webTestClient
        .get()
        .uri("/actuator/metrics")
        .accept(MediaType.ALL)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .value(content -> assertFalse(content.isBlank()));
  }

  /**
   * Gets metrics and expect unauthorized.
   */
  @Test
  void getMetricsAndExpectUnauthorized() {
    webTestClient
        .get()
        .uri("/actuator/metrics")
        .accept(MediaType.ALL)
        .exchange()
        .expectStatus().isUnauthorized();
  }

}
