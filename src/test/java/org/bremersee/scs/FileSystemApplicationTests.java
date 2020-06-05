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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bremersee.scs.config.ScsProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * The file system application tests.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "bremersee.scs.pattern=/**",
    "bremersee.scs.directory-pattern-index-map.[/dir/**]:dir.html",
    "bremersee.scs.directory-pattern-index-map.[/**]:index.html"
})
@ActiveProfiles({"in-memory"})
@TestInstance(Lifecycle.PER_CLASS)
@Slf4j
class FileSystemApplicationTests {

  private final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

  private final List<File> files = new ArrayList<>();

  /**
   * The web test client.
   */
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  WebTestClient webTestClient;

  /**
   * The static content properties.
   */
  @Autowired
  ScsProperties properties;

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @BeforeAll
  void setUp() throws Exception {
    try {
      File tmpDir = new File(System.getProperty("java.io.tmpdir"));
      File root = File.createTempFile("scs", "content", tmpDir);
      if (!(root.delete() && root.mkdir())) {
        throw new IOException("Creating root failed.");
      }
      files.add(root);

      File rootIndex = new File(root, "index.html");
      IOUtils.copy(
          resourceLoader.getResource("classpath:content/index.html").getInputStream(),
          new FileOutputStream(rootIndex));
      files.add(rootIndex);

      File example = new File(root, "example.html");
      IOUtils.copy(
          resourceLoader.getResource("classpath:content/example.html").getInputStream(),
          new FileOutputStream(example));
      files.add(example);

      File dir = new File(root, "dir");
      if (!dir.mkdir()) {
        throw new IOException("Creating dir failed.");
      }
      files.add(dir);

      File dirIndex = new File(dir, "dir.html");
      IOUtils.copy(
          resourceLoader.getResource("classpath:content/dir/dir.html").getInputStream(),
          new FileOutputStream(dirIndex));
      files.add(dirIndex);

      properties.setContentLocation(root.getAbsolutePath());

    } catch (Exception e) {
      deleteFiles();
      throw e;
    }
  }

  /**
   * Shut down.
   */
  @AfterAll
  void shutDown() {
    deleteFiles();
  }

  private void deleteFiles() {
    files.sort((f1, f2) -> {
      if (f1.isDirectory() && !f2.isDirectory()) {
        return -1;
      }
      if (!f1.isDirectory() && f2.isDirectory()) {
        return 1;
      }
      return f1.getName().compareTo(f2.getName());
    });

    for (File file : files) {
      try {
        if (file.exists() && !file.delete()) {
          log.warn("Test file was not deleted: {}", file);
        }
      } catch (Exception e) {
        log.warn("Test file was not deleted: {}", file);
      }
    }
  }

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

}
