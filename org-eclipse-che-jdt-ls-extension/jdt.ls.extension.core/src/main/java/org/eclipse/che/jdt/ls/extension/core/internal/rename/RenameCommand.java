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
package org.eclipse.che.jdt.ls.extension.core.internal.rename;

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
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.rename.RenameProcessor;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.WorkspaceEdit;

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
              preferencesManager,
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

      RenameProcessor processor = createRenameProcessor(curr);
      processor.renameOccurrences(edit, params.getNewName(), pm);
    } catch (CoreException ex) {
      JavaLanguageServerPlugin.logException(
          "Problem with rename for " + params.getTextDocument().getUri(), ex);
    }

    return edit;
  }

  private static RenameProcessor createRenameProcessor(IJavaElement selectedElement)
      throws JavaModelException {
    if (selectedElement instanceof IType) {
      return new RenameTypeProcessor(selectedElement, preferencesManager);
    }
    if (selectedElement instanceof IMethod && ((IMethod) selectedElement).isConstructor()) {
      return new RenameTypeProcessor(
          ((IMethod) selectedElement).getDeclaringType(), preferencesManager);
    }
    return new RenameProcessor(selectedElement, preferencesManager);
  }

  private static void validateArguments(List<Object> arguments) {
    Preconditions.checkArgument(
        !arguments.isEmpty(), RenameCommand.class.getName() + " is expected.");
  }
}
