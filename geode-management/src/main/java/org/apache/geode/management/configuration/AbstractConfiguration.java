/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.geode.management.configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.lang3.StringUtils;

import org.apache.geode.annotations.Experimental;
import org.apache.geode.lang.Identifiable;
import org.apache.geode.management.api.JsonSerializable;

@Experimental
public abstract class AbstractConfiguration
    implements Identifiable<String>, Serializable, JsonSerializable {
  public static final String CLUSTER = "cluster";

  protected List<String> groups = new ArrayList<>();

  /**
   * this returns a non-null value
   * for cluster level element, it will return "cluster" for sure.
   */
  @JsonIgnore
  public String getConfigGroup() {
    String group = getGroup();
    if (StringUtils.isBlank(group))
      return CLUSTER;
    else
      return group;
  }

  /**
   * this returns the first group set by the user
   * if no group is set, this returns null
   */
  @JsonIgnore
  public String getGroup() {
    if (groups.size() == 0) {
      return null;
    }
    return groups.get(0);
  }

  @JsonSetter
  public void setGroup(String group) {
    groups.clear();

    if (StringUtils.isBlank(group)) {
      return;
    }
    groups.add(group);
  }

  public List<String> getGroups() {
    return groups;
  }

  public void addGroup(String group) {
    groups.add(group);
  }
}