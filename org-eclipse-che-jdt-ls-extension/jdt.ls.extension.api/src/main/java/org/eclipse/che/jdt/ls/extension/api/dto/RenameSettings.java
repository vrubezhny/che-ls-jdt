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

import org.eclipse.lsp4j.RenameParams;

/**
 * Settings for all Rename refactorings.
 *
 * @author Valeriy Svydenko
 */
public class RenameSettings {
  private RenameParams renameParams;
  private boolean isDelegateUpdating;
  private boolean isDeprecateDelegates;
  private boolean isUpdateQualifiedNames;
  private boolean isUpdateSubpackages;
  private boolean isUpdateReferences;
  private boolean isUpdateSimilarDeclarations;
  private boolean isUpdateTextualMatches;
  private int machStrategy;
  private String filePatterns;

  /** Information about changed element {@link RenameParams} */
  public void setRenameParams(RenameParams renameParams) {
    this.renameParams = renameParams;
  }

  public RenameParams getRenameParams() {
    return renameParams;
  }

  // IDelegateUpdating
  /**
   * If refactoring object is capable of creating appropriate delegates for the refactored elements.
   * This value used to set whether to create delegates.
   */
  public boolean isDelegateUpdating() {
    return isDelegateUpdating;
  }

  public void setDelegateUpdating(boolean isDelegateUpdating) {
    this.isDelegateUpdating = isDelegateUpdating;
  }

  /**
   * If refactoring object is capable of creating appropriate delegates for the refactored elements.
   * This value used to set whether to deprecate delegates.
   */
  public boolean isDeprecateDelegates() {
    return isDeprecateDelegates;
  }

  public void setDeprecateDelegates(boolean delegates) {
    this.isDeprecateDelegates = isDeprecateDelegates;
  }

  // IQualifiedNameUpdating

  /**
   * If this refactoring object is capable of updating qualified names in non Java files. then this
   * value is used to inform the refactoring object whether references in non Java files should be
   * updated.
   */
  public boolean isUpdateQualifiedNames() {
    return isUpdateQualifiedNames;
  }

  public void setUpdateQualifiedNames(boolean update) {
    this.isUpdateQualifiedNames = isUpdateQualifiedNames;
  }

  public String getFilePatterns() {
    return filePatterns;
  }

  public void setFilePatterns(String filePatterns) {
    this.filePatterns = filePatterns;
  }

  // ISubpackagesUpdating

  /**
   * Informs the refactoring object whether subpackages should be updated. This value used to set
   * whether to updating packages.
   *
   * @return <code>true</code> if subpackages updating is enabled
   */
  public boolean isUpdateSubpackages() {
    return isUpdateSubpackages;
  }

  public void setUpdateSubpackages(boolean isUpdateSubpackages) {
    this.isUpdateSubpackages = isUpdateSubpackages;
  }

  // IReferenceUpdating

  /**
   * Informs the refactoring object whether references should be updated. * @return <code>true
   * </code> iff reference updating is enabled
   */
  public boolean isUpdateReferences() {
    return isUpdateReferences;
  }

  public void setUpdateReferences(boolean update) {
    this.isUpdateReferences = isUpdateReferences;
  }

  // ISimilarDeclarationUpdating

  /**
   * If this refactoring object is capable of updating similar declarations of the renamed element,
   * then this value is used to inform the refactoring object whether similar declarations should be
   * updated.
   *
   * @return
   */
  public boolean isUpdateSimilarDeclarations() {
    return isUpdateSimilarDeclarations;
  }

  public void setUpdateSimilarDeclarations(boolean isUpdateSimilarDeclarations) {
    this.isUpdateSimilarDeclarations = isUpdateSimilarDeclarations;
  }

  /** method is used to set the match strategy for determining similarly named elements. */
  public int getMachStrategy() {
    return machStrategy;
  }

  /**
   * @param strategy must be one of {@link
   *     org.eclipse.che.ide.ext.java.shared.dto.refactoring.RenameSettings.MachStrategy} values.
   */
  public void setMachStrategy(int machStrategy) {
    this.machStrategy = machStrategy;
  }

  // ITextUpdating

  /**
   * This method is used to inform the refactoring object whether references in regular (non
   * JavaDoc) comments and string literals should be updated.
   */
  public boolean isUpdateTextualMatches() {
    return isUpdateTextualMatches;
  }

  public void setUpdateTextualMatches(boolean isUpdateTextualMatches) {
    this.isUpdateTextualMatches = isUpdateTextualMatches;
  }

  enum MachStrategy {
    EXACT(1),
    EMBEDDED(2),
    SUFFIX(3);
    private int value;

    MachStrategy(int i) {
      value = i;
    }

    public int getValue() {
      return value;
    }
  }
}
