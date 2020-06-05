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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.PathContainer;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.test.StepVerifier;

/**
 * The static content resource lookup function test.
 *
 * @author Christian Bremer
 */
class ScsResourceLookupFunctionTest {

  /**
   * With non existing content location.
   *
   * @throws Exception the exception
   */
  @Test
  void withNonExistingContentLocation() throws Exception {
    File tmp = new File(System.getProperty("java.io.tmpdir"));
    File root = File.createTempFile("scs", "content", tmp);
    if (!root.delete()) {
      throw new IOException("Creating content location failed.");
    }
    ScsProperties properties = new ScsProperties();
    ScsResourceLookupFunction lookup = new ScsResourceLookupFunction(properties);

    PathContainer pathContainer = PathContainer.parsePath("/");
    ServerRequest request = mock(ServerRequest.class);
    when(request.pathContainer()).thenReturn(pathContainer);

    StepVerifier.create(lookup.apply(request))
        .assertNext(resource -> {
          assertNotNull(resource);
          assertEquals("index.html", resource.getFilename());
        })
        .verifyComplete();
  }

  /**
   * With encoded path.
   */
  @Test
  void withEncodedPath() {
    ScsProperties properties = new ScsProperties();
    properties.setContentLocation("classpath:content");
    ScsResourceLookupFunction lookup = new ScsResourceLookupFunction(properties);

    PathContainer pathContainer = PathContainer.parsePath("/stra%C3%9Fe/enc.html");
    ServerRequest request = mock(ServerRequest.class);
    when(request.pathContainer()).thenReturn(pathContainer);

    StepVerifier.create(lookup.apply(request))
        .assertNext(resource -> {
          assertNotNull(resource);
          assertEquals("enc.html", resource.getFilename());
        })
        .verifyComplete();
  }

  /**
   * With empty path and non existing resource.
   */
  @Test
  void withEmptyPathAndNonExistingResource() {
    ScsProperties properties = new ScsProperties();
    properties.setContentLocation("classpath:content");
    properties.setDefaultIndex("not-exists.html");
    ScsResourceLookupFunction lookup = new ScsResourceLookupFunction(properties);

    PathContainer pathContainer = PathContainer.parsePath("/");
    ServerRequest request = mock(ServerRequest.class);
    when(request.pathContainer()).thenReturn(pathContainer);

    StepVerifier.create(lookup.apply(request))
        .verifyComplete();
  }

  /**
   * With non existing resource.
   */
  @Test
  void withNonExistingResource() {
    ScsProperties properties = new ScsProperties();
    properties.setContentLocation("classpath:content");
    properties.setDefaultIndex("not-exists.html");
    ScsResourceLookupFunction lookup = new ScsResourceLookupFunction(properties);

    PathContainer pathContainer = PathContainer.parsePath("/dir/not-exists.html");
    ServerRequest request = mock(ServerRequest.class);
    when(request.pathContainer()).thenReturn(pathContainer);

    StepVerifier.create(lookup.apply(request))
        .verifyComplete();
  }

}