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

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.eclipse.che.jdt.ls.extension.api.dto.LinkedModeModel;
import org.eclipse.che.jdt.ls.extension.api.dto.LinkedModelParams;
import org.eclipse.che.jdt.ls.extension.api.dto.LinkedPositionGroup;
import org.eclipse.che.jdt.ls.extension.api.dto.Region;
import org.eclipse.che.jdt.ls.extension.api.dto.RenameSelectionParams;
import org.eclipse.che.jdt.ls.extension.core.internal.GsonUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.ls.core.internal.corext.refactoring.util.RefactoringASTParser;

/**
 * Command to to prepare linked model for rename refactoring.
 *
 * @author Valeriy Svydenko
 */
public class GetLinkedModelCommand {
  private static final Gson GSON = GsonUtils.getInstance();

  /**
   * Analyzes cursor position and prepares linked model fro rename refactoring.
   *
   * @param arguments contains instance of {@link RenameSelectionParams}
   * @param pm progress monitor
   * @return model of linked mode
   */
  public static LinkedModeModel execute(List<Object> arguments, IProgressMonitor pm) {
    validateArguments(arguments);

    LinkedModelParams params =
        GSON.fromJson(GSON.toJson(arguments.get(0)), LinkedModelParams.class);

    ICompilationUnit cu = JDTUtils.resolveCompilationUnit(params.getUri());
    if (cu == null) {
      return null;
    }

    CompilationUnit root =
        new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL).parse(cu, true);

    LinkedPositionGroup group = new LinkedPositionGroup();
    ASTNode selectedNode = NodeFinder.perform(root, params.getOffset(), 0);
    if (!(selectedNode instanceof SimpleName)) {
      return null;
    }
    SimpleName nameNode = (SimpleName) selectedNode;

    final int pos = nameNode.getStartPosition();
    ASTNode[] sameNodes = LinkedNodeFinder.findByNode(root, nameNode);

    Arrays.sort(
        sameNodes,
        new Comparator<ASTNode>() {
          public int compare(ASTNode o1, ASTNode o2) {
            return rank(o1) - rank(o2);
          }

          private int rank(ASTNode node) {
            int relativeRank = node.getStartPosition() + node.getLength() - pos;
            if (relativeRank < 0) {
              return Integer.MAX_VALUE + relativeRank;
            } else {
              return relativeRank;
            }
          }
        });
    for (ASTNode elem : sameNodes) {
      Region region = new Region();
      region.setOffset(elem.getStartPosition());
      region.setLength(elem.getLength());
      group.addPosition(region);
    }
    LinkedModeModel model = new LinkedModeModel();
    model.addGroup(group);
    return model;
  }

  private static void validateArguments(List<Object> arguments) {
    Preconditions.checkArgument(
        !arguments.isEmpty(), RenameSelectionParams.class.getName() + " is expected.");
  }
}
