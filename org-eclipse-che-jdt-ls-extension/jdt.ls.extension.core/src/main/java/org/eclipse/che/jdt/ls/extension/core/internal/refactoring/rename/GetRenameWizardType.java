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

import static org.eclipse.che.jdt.ls.extension.core.internal.Utils.ensureNotCancelled;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import java.util.List;
import org.eclipse.che.jdt.ls.extension.api.RenameType;
import org.eclipse.che.jdt.ls.extension.api.dto.RenameSelectionParams;
import org.eclipse.che.jdt.ls.extension.api.dto.RenameWizardType;
import org.eclipse.che.jdt.ls.extension.core.internal.GsonUtils;
import org.eclipse.che.jdt.ls.extension.core.internal.JavaModelUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;

/**
 * Returns information about renamed type.
 *
 * @author Valeriy Svydenko
 */
public class GetRenameWizardType {
  private static final Gson GSON = GsonUtils.getInstance();

  /**
   * Analyzes curesor position and finds type of selected element.
   *
   * @param arguments contains instance of {@link RenameSelectionParams}
   * @param pm progress monitor
   * @return information about selected element
   */
  public static RenameWizardType execute(List<Object> arguments, IProgressMonitor pm) {
    validateArguments(arguments);

    ensureNotCancelled(pm);

    RenameWizardType wizardType = new RenameWizardType();

    RenameSelectionParams params =
        GSON.fromJson(GSON.toJson(arguments.get(0)), RenameSelectionParams.class);

    try {
      RenameType selectedElement = params.getRenameType();
      if (RenameType.JAVA_ELEMENT.equals(selectedElement)) {
        IJavaElement element =
            JavaModelUtil.getJavaElement(params.getPosition(), params.getResourceUri(), pm);
        if (element == null) {
          wizardType.setRenameType(RenameType.UNKNOWN);
          return wizardType;
        }
        wizardType.setElementName(element.getElementName());
        wizardType.setRenameType(getWizardType(element));
      } else if (RenameType.COMPILATION_UNIT.equals(selectedElement)) {
        wizardType.setRenameType(selectedElement);
        ICompilationUnit cu = JDTUtils.resolveCompilationUnit(params.getResourceUri());
        wizardType.setElementName(cu.getElementName());
      } else if (RenameType.PACKAGE.equals(selectedElement)) {
        wizardType.setRenameType(selectedElement);
        IPackageFragment pack = JDTUtils.resolvePackage(params.getResourceUri());
        wizardType.setElementName(pack.getElementName());
      }

      return wizardType;
    } catch (CoreException e) {
      JavaLanguageServerPlugin.logException(e.getMessage(), e);
    }

    return wizardType;
  }

  private static RenameType getWizardType(IJavaElement element) throws CoreException {
    if (element == null) {
      return null;
    }
    switch (element.getElementType()) {
      case IJavaElement.PACKAGE_FRAGMENT:
        return RenameType.PACKAGE;
      case IJavaElement.COMPILATION_UNIT:
        return RenameType.COMPILATION_UNIT;
      case IJavaElement.TYPE:
        return RenameType.TYPE;
      case IJavaElement.METHOD:
        final IMethod method = (IMethod) element;
        if (method.isConstructor()) {
          return RenameType.TYPE;
        } else {
          return RenameType.METHOD;
        }
      case IJavaElement.FIELD:
        if (JdtFlags.isEnum((IMember) element)) {
          return RenameType.ENUM_CONSTANT;
        }
        return RenameType.FIELD;
      case IJavaElement.TYPE_PARAMETER:
        return RenameType.TYPE_PARAMETER;
      case IJavaElement.LOCAL_VARIABLE:
        return RenameType.LOCAL_VARIABLE;
    }
    return null;
  }

  private static void validateArguments(List<Object> arguments) {
    Preconditions.checkArgument(
        !arguments.isEmpty(), RenameSelectionParams.class.getName() + " is expected.");
  }
}
