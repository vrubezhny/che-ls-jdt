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

public class LinkedPositionGroup {
  private LinkedData linkedData;
  private List<Region> positions;

  public LinkedPositionGroup() {
    positions = new ArrayList<Region>();
  }

  public LinkedData getLinkedData() {
    return linkedData;
  }

  public void setLinkedData(LinkedData linkedData) {
    this.linkedData = linkedData;
  }

  public List<Region> getPositions() {
    return positions;
  }

  public void setPositions(List<Region> positions) {
    this.positions = positions;
  }

  public void addPosition(Region region) {
    positions.add(region);
  }
}
