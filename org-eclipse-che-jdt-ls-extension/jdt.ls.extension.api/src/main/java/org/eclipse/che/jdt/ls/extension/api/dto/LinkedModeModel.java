/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.jdt.ls.extension.api.dto;

import java.util.ArrayList;
import java.util.List;

public class LinkedModeModel {
  private List<LinkedPositionGroup> groups;
  private int escapePosition;

  public LinkedModeModel() {
    groups = new ArrayList<>();
  }

  public List<LinkedPositionGroup> getGroups() {
    return groups;
  }

  public void setGroups(List<LinkedPositionGroup> groups) {
    this.groups = groups;
  }

  public void addGroup(LinkedPositionGroup group) {
    groups.add(group);
  }

  public int getEscapePosition() {
    return escapePosition;
  }

  public void setEscapePosition(int escapePosition) {
    this.escapePosition = escapePosition;
  }
}
