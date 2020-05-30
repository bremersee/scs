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

import org.junit.jupiter.api.Disabled;
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
    "bremersee.scs.content-location=classpath:content/",
    "bremersee.scs.root-resource=index.html"
})
@ActiveProfiles({"in-memory"})
@Disabled
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
   * Gets example content.
   */
  @Test
  void getExampleContent() {
    webTestClient
        .get()
        .uri("/example.html")
        .accept(MediaType.ALL)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .value(content -> assertTrue(content.contains("Example content.")));
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
