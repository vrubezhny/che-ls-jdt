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
package org.eclipse.che.jdt.ls.extension.core.internal.refactoring.rename;

import static org.eclipse.jdt.ls.core.internal.corext.refactoring.changes.ChangeUtil.convertChanges;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.che.jdt.ls.extension.api.dto.RenameSettings;
import org.eclipse.che.jdt.ls.extension.core.internal.GsonUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.rename.JavaRenameProcessor;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.rename.RenamePackageProcessor;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.rename.RenameSupport;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.tagging.ISimilarDeclarationUpdating;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.tagging.ITextUpdating;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

/** @author Valeriy Svydenko */
public class RenameCommand {

  private static PreferenceManager preferencesManager =
      JavaLanguageServerPlugin.getPreferencesManager();
  private static final Gson GSON = GsonUtils.getInstance();

  /**
   * The command for doing Rename refactoring.
   *
   * @param arguments {@link RenameParams} expected
   */
  public static WorkspaceEdit execute(List<Object> arguments, IProgressMonitor pm) {
    validateArguments(arguments);
    WorkspaceEdit edit = new WorkspaceEdit();

    RenameSettings renameSetings =
        GSON.fromJson(GSON.toJson(arguments.get(0)), RenameSettings.class);
    RenameParams params = renameSetings.getRenameParams();

    try {
      final ICompilationUnit unit =
          JDTUtils.resolveCompilationUnit(params.getTextDocument().getUri());

      IJavaElement[] elements =
          JDTUtils.findElementsAtSelection(
              unit,
              params.getPosition().getLine(),
              params.getPosition().getCharacter(),
              JavaLanguageServerPlugin.getPreferencesManager(),
              pm);
      if (elements == null || elements.length == 0) {
        return edit;
      }
      IJavaElement curr = null;
      if (elements.length != 1) {
        // they could be package fragments.
        // We need to select the one that matches the package fragment of the current
        // unit
        IPackageFragment packageFragment = (IPackageFragment) unit.getParent();
        IJavaElement found =
            Stream.of(elements).filter(e -> e.equals(packageFragment)).findFirst().orElse(null);
        if (found == null) {
          // this would be a binary package fragment
          curr = elements[0];
        } else {
          curr = found;
        }
      } else {
        curr = elements[0];
      }

      RenameSupport renameSupport =
          RenameSupport.create(curr, params.getNewName(), RenameSupport.UPDATE_REFERENCES);
      JavaRenameProcessor javaRenameProcessor = renameSupport.getJavaRenameProcessor();
      RenameRefactoring renameRefactoring = renameSupport.getRenameRefactoring();
      setSettings(renameSetings, renameRefactoring);

      CreateChangeOperation create =
          new CreateChangeOperation(
              new CheckConditionsOperation(
                  renameRefactoring, CheckConditionsOperation.ALL_CONDITIONS),
              RefactoringStatus.FATAL);
      create.run(pm);
      Change change = create.getChange();
      if (change == null) {
        return edit;
      }

      convertChanges(change, edit, JavaLanguageServerPlugin.getPreferencesManager());
    } catch (CoreException ex) {
      JavaLanguageServerPlugin.logException(
          "Problem with rename for " + params.getTextDocument().getUri(), ex);
    }

    return edit;
  }

  private static void setSettings(RenameSettings settings, RenameRefactoring refactoring) {
    RefactoringProcessor processor = refactoring.getProcessor();
    if (processor instanceof RenamePackageProcessor) {
      ((RenamePackageProcessor) processor).setRenameSubpackages(settings.isUpdateSubpackages());
    }
    IDelegateUpdating delegateUpdating =
        (IDelegateUpdating) refactoring.getAdapter(IDelegateUpdating.class);
    if (delegateUpdating != null && delegateUpdating.canEnableDelegateUpdating()) {
      delegateUpdating.setDelegateUpdating(settings.isDelegateUpdating());
      delegateUpdating.setDeprecateDelegates(settings.isDeprecateDelegates());
    }
    IQualifiedNameUpdating nameUpdating =
        (IQualifiedNameUpdating) refactoring.getAdapter(IQualifiedNameUpdating.class);
    if (nameUpdating != null && nameUpdating.canEnableQualifiedNameUpdating()) {
      nameUpdating.setUpdateQualifiedNames(settings.isUpdateQualifiedNames());
      if (settings.isUpdateQualifiedNames()) {
        nameUpdating.setFilePatterns(settings.getFilePatterns());
      }
    }

    IReferenceUpdating referenceUpdating =
        (IReferenceUpdating) refactoring.getAdapter(IReferenceUpdating.class);
    if (referenceUpdating != null) {
      referenceUpdating.setUpdateReferences(settings.isUpdateReferences());
    }

    ISimilarDeclarationUpdating similarDeclarationUpdating =
        (ISimilarDeclarationUpdating) refactoring.getAdapter(ISimilarDeclarationUpdating.class);
    if (similarDeclarationUpdating != null) {
      similarDeclarationUpdating.setUpdateSimilarDeclarations(
          settings.isUpdateSimilarDeclarations());
      if (settings.isUpdateSimilarDeclarations()) {
        similarDeclarationUpdating.setMatchStrategy(settings.getMachStrategy());
      }
    }

    ITextUpdating textUpdating = (ITextUpdating) refactoring.getAdapter(ITextUpdating.class);
    if (textUpdating != null && textUpdating.canEnableTextUpdating()) {
      textUpdating.setUpdateTextualMatches(settings.isUpdateTextualMatches());
    }
  }

  private static void validateArguments(List<Object> arguments) {
    Preconditions.checkArgument(
        !arguments.isEmpty(), RenameCommand.class.getName() + " is expected.");
  }
}
