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

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * The authentication properties.
 */
@ConfigurationProperties(prefix = "bremersee.access")
@Getter
@Setter
@ToString(exclude = {"actuatorUserPassword", "adminUserPassword"})
@EqualsAndHashCode
public class AuthenticationProperties {

  private static final String ROLE_ACTUATOR = "ROLE_ACTUATOR";

  private String actuatorRoleName;

  private String actuatorUserName;

  private String actuatorUserPassword;

  private String adminUserName;

  private String adminUserPassword;

  /**
   * Gets actuator role name.
   *
   * @return the actuator role name
   */
  public String getActuatorRoleName() {
    return StringUtils.hasText(actuatorRoleName) ? actuatorRoleName : ROLE_ACTUATOR;
  }

  /**
   * Build users.
   *
   * @return the users
   */
  List<SimpleUser> buildUsers() {
    List<SimpleUser> users = new ArrayList<>(3);
    if (StringUtils.hasText(actuatorUserName)) {
      users.add(new SimpleUser(actuatorUserName, actuatorUserPassword, getActuatorRoleName()));
    }
    if (StringUtils.hasText(adminUserName)) {
      users.add(new SimpleUser(adminUserName, adminUserPassword, getActuatorRoleName()));
    }
    return users;
  }

  /**
   * The simple user.
   */
  @Getter
  @Setter
  @ToString(exclude = "password")
  @EqualsAndHashCode(exclude = "password")
  @NoArgsConstructor
  static class SimpleUser implements Serializable, Principal {

    private static final long serialVersionUID = -1393400622632455935L;

    private String name;

    private String password;

    private List<String> authorities = new ArrayList<>();

    /**
     * Instantiates a new simple user.
     *
     * @param name the name
     * @param password the password
     * @param authorities the authorities
     */
    SimpleUser(String name, String password, String... authorities) {
      this.name = name;
      this.password = password;
      if (authorities != null) {
        Collections.addAll(this.authorities, authorities);
      }
    }
  }

}
