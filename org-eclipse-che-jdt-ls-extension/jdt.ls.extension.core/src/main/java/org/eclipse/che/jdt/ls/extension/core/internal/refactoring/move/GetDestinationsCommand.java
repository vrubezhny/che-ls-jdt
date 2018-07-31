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
package org.eclipse.che.jdt.ls.extension.core.internal.refactoring.move;

import static org.eclipse.che.jdt.ls.extension.core.internal.Utils.ensureNotCancelled;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.che.jdt.ls.extension.api.dto.JavaProjectStructure;
import org.eclipse.che.jdt.ls.extension.api.dto.PackageFragment;
import org.eclipse.che.jdt.ls.extension.api.dto.PackageFragmentRoot;
import org.eclipse.che.jdt.ls.extension.core.internal.JavaModelUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ExternalFoldersManager;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;

/**
 * The command to get all available destinations.
 *
 * @author Valeriy Svydenko
 */
public class GetDestinationsCommand {
  /**
   * Find all possible destinations.
   *
   * @return list of the destinations
   */
  public static List<JavaProjectStructure> execute(List<Object> arguments, IProgressMonitor pm) {
    ensureNotCancelled(pm);

    List<IJavaProject> workspaceJavaProjects = JavaModelUtil.getWorkspaceJavaProjects();
//    try {
//		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
//	} catch (CoreException e2) {
//		e2.printStackTrace();
//	}
    List<JavaProjectStructure> result = new ArrayList<>();
    for (IJavaProject javaProject : workspaceJavaProjects) {
      if (javaProject.exists()) {
        JavaProjectStructure project = new JavaProjectStructure();
        String projectUri = JDTUtils.getFileURI(javaProject.getResource());
        project.setName(projectUri.substring(projectUri.lastIndexOf(JDTUtils.PATH_SEPARATOR) + 1));
        project.setUri(projectUri);
        try {
          project.setPackageRoots(toPackageRoots(javaProject, projectUri));
        } catch (CoreException e) {
          JavaLanguageServerPlugin.logException(e.getMessage(), e);
        }
        result.add(project);
      }
    }
    return result;
  }

  private static List<PackageFragmentRoot> toPackageRoots(
      IJavaProject javaProject, String projectUri) throws CoreException {
    JavaLanguageServerPlugin.logInfo(
        ">>> GetDestinationsCommand.toPackageRoots("
            + javaProject.getElementName()
            + ", "
            + projectUri
            + "): start");
    JavaLanguageServerPlugin.logInfo(
        ">> GetDestinationsCommand.toPackageRoots(): project: "
            + (javaProject == null
                ? "NULL"
                : javaProject.getElementName()
                    + " ("
                    + javaProject.getProject().getLocation().toString()
                    + ")")
            + ", hash: "
            + (javaProject == null ? "NULL" : javaProject.hashCode())
            + "["
            + System.currentTimeMillis()
            + "]");
    
    IClasspathEntry[] cp = ((JavaProject)javaProject).getResolvedClasspath();
    JavaLanguageServerPlugin.logInfo(
            ">>> GetDestinationsCommand.toPackageRoots("
                + javaProject.getElementName()
                + ", "
                + projectUri
                + "): resolved CP: start");
    for (IClasspathEntry e : cp) {
    	if (e.getEntryKind() == 3) {
    		JavaLanguageServerPlugin.logInfo(
                ">>>\t\tCP Entry: EntryK: " 
        		+ e.getEntryKind() + ", ContentK: "
                    + e.getContentKind()
                    + ", "
                    + e.getPath().toString()
        		);
    		IPath projectPath =  javaProject.getProject().getFullPath();
    		IPath entryPath = e.getPath();
    		JavaLanguageServerPlugin.logInfo(
                    ">>>\t\tCP Entry: EntryK: " 
            		+ e.getEntryKind() + ", ContentK: "
                        + e.getContentKind()
                        + ", "
                        + e.getPath().toString()
                        + ":\n\tjavaProject.getProject().getFullPath(): " + projectPath.toString()
                        + ":\n\te.getPath();: " + entryPath.toString() 
                        + ":\n\tprojectPath.isPrefixOf(entryPath): " + (projectPath.isPrefixOf(entryPath) ? "YES":"NO") 
            		);
   		
    		if (projectPath.isPrefixOf(entryPath)){
				Object target = getTarget(entryPath, true/*check existency*/);
				if (target == null) {
		    		JavaLanguageServerPlugin.logInfo(
		                    ">>>\t\tCP Entry: EntryK: " 
		            		+ e.getEntryKind() + ", ContentK: "
		                        + e.getContentKind()
		                        + ", "
		                        + e.getPath().toString()
		                        + ": TARGET IS NULL"
		            		);
				}
				else if (target instanceof IFolder || target instanceof IProject){
					IPackageFragmentRoot root = null;
		    		JavaLanguageServerPlugin.logInfo(
		                    ">>>\t\tCP Entry: EntryK: " 
		            		+ e.getEntryKind() + ", ContentK: "
		                        + e.getContentKind()
		                        + ", "
		                        + e.getPath().toString()
		                        + ": TARGER IS IFile or IProject - call:  getPackageFragmentRoot((IResource)target);"
		            		);
					root = javaProject.getPackageFragmentRoot((IResource)target);
		    		JavaLanguageServerPlugin.logInfo(
		                    ">>>\t\tCP Entry: EntryK: " 
		            		+ e.getEntryKind() + ", ContentK: "
		                        + e.getContentKind()
		                        + ", "
		                        + e.getPath().toString()
		                        + ": ROOT is found: " 
		                        + (root == null? "NO" : "YES : " + root.getElementName())
		                   );
				}
			}
    	}
   	
    }
    JavaLanguageServerPlugin.logInfo(
            ">>> GetDestinationsCommand.toPackageRoots("
                + javaProject.getElementName()
                + ", "
                + projectUri
                + "): resolved CP: end");
    
    IPackageFragmentRoot[] packageFragmentRoots = javaProject.getAllPackageFragmentRoots();
    List<PackageFragmentRoot> result = new ArrayList<>();
    for (IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
            JavaLanguageServerPlugin.logInfo(
                ">>> GetDestinationsCommand.toPackageRoots(): javaProject.getPath(): "
                    + javaProject.getPath()
                    + ", is prefixOf("
                    + packageFragmentRoot.getPath()
                    + ") : "
                    + (javaProject.getPath().isPrefixOf(packageFragmentRoot.getPath()) ? "yes" : "no")
                    + ", is K_SOURCE: "
                    + (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE ? "yes" : "no"));

      if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE
          && javaProject.getPath().isPrefixOf(packageFragmentRoot.getPath())) {
          JavaLanguageServerPlugin.logInfo(
                  ">>> GetDestinationsCommand.toPackageRoots(): ACCEPTED: packageFragmentRoot: "
                      + packageFragmentRoot.getElementName()
                      + ", is K_SOURCE: "
                      + (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE ? "yes" : "no"));        PackageFragmentRoot root = new PackageFragmentRoot();
        root.setUri(JDTUtils.getFileURI(packageFragmentRoot.getResource()));
        root.setProjectUri(projectUri);
        root.setPackages(toPackageFragments(packageFragmentRoot, projectUri));
        result.add(root);
      } else {
        JavaLanguageServerPlugin.logInfo(
            ">>> GetDestinationsCommand.toPackageRoots(): NOT ACCEPTED: packageFragmentRoot: "
                + packageFragmentRoot.getElementName()
                + ", is K_SOURCE: "
                + (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE ? "yes" : "no"));
      }
    }
    JavaLanguageServerPlugin.logInfo(
        ">>> GetDestinationsCommand.toPackageRoots("
            + javaProject.getElementName()
            + ", "
            + projectUri
            + "): end");
    return result;
  }

  public static Object getTarget(IPath path, boolean checkResourceExistence) {
	    JavaLanguageServerPlugin.logInfo(
	            ">>> GetDestinationsCommand.getTarget("
	                + path.toString()
	                + ", "
	                + "checkResourceExistance:" + (checkResourceExistence?"YES":"NO")
	                + "): start");
		Object target = getWorkspaceTarget(path); // Implicitly checks resource existence
		if (target != null) {
		    JavaLanguageServerPlugin.logInfo(
		            ">>> GetDestinationsCommand.getTarget("
		                + path.toString()
		                + ", "
		                + "checkResourceExistance:" + (checkResourceExistence?"YES":"NO")
		                + "): done getWorkspaceTarget(): " + String.valueOf(target));
			
			return target;
		}
		target = getExternalTarget(path, checkResourceExistence);
	    JavaLanguageServerPlugin.logInfo(
	            ">>> GetDestinationsCommand.getTarget("
	                + path.toString()
	                + ", "
	                + "checkResourceExistance:" + (checkResourceExistence?"YES":"NO")
	                + "): done getExternalTarget: " + String.valueOf(target));
		
		return target;
	}
  
  public static Object getExternalTarget(IPath path, boolean checkResourceExistence) {
	    JavaLanguageServerPlugin.logInfo(
	            ">>> GetDestinationsCommand.getExternalTarget("
	                + path.toString()
	                + ", "
	                + "checkResourceExistance:" + (checkResourceExistence?"YES":"NO")
	                + "): start");
		if (path == null) {
		    JavaLanguageServerPlugin.logInfo(
		            ">>> GetDestinationsCommand.getExternalTarget("
		                + path.toString()
		                + ", "
		                + "checkResourceExistance:" + (checkResourceExistence?"YES":"NO")
		                + "): done: [path == null]");
			return null;
		}
		ExternalFoldersManager externalFoldersManager = JavaModelManager.getExternalManager();
		Object linkedFolder = externalFoldersManager.getFolder(path);
		if (linkedFolder != null) {
			if (checkResourceExistence) {
				// check if external folder is present
				File externalFile = new File(path.toOSString());
				if (!externalFile.isDirectory()) {
				    JavaLanguageServerPlugin.logInfo(
				            ">>> GetDestinationsCommand.getExternalTarget("
				                + path.toString()
				                + ", "
				                + "checkResourceExistance:" + (checkResourceExistence?"YES":"NO")
				                + "): done: [!externalFile.isDirectory()]");
					return null;
				}
			}
		    JavaLanguageServerPlugin.logInfo(
		            ">>> GetDestinationsCommand.getExternalTarget("
		                + path.toString()
		                + ", "
		                + "checkResourceExistance:" + (checkResourceExistence?"YES":"NO")
		                + "): done: return linkedFolder: " + String.valueOf(linkedFolder));
			return linkedFolder;
		}
		File externalFile = new File(path.toOSString());
		if (!checkResourceExistence) {
		    JavaLanguageServerPlugin.logInfo(
		            ">>> GetDestinationsCommand.getExternalTarget("
		                + path.toString()
		                + ", "
		                + "checkResourceExistance:" + (checkResourceExistence?"YES":"NO")
		                + "): done: (!checkResourceExistence) return externalFile: " + String.valueOf(externalFile));			
			return externalFile;
		} else if (isExternalFile(path)) {
		    JavaLanguageServerPlugin.logInfo(
		            ">>> GetDestinationsCommand.getExternalTarget("
		                + path.toString()
		                + ", "
		                + "checkResourceExistance:" + (checkResourceExistence?"YES":"NO")
		                + "): done: (isExternalFile(path)) return externalFile: " + String.valueOf(externalFile));
			return externalFile;
		}
	    JavaLanguageServerPlugin.logInfo(
	            ">>> GetDestinationsCommand.getExternalTarget("
	                + path.toString()
	                + ", "
	                + "checkResourceExistance:" + (checkResourceExistence?"YES":"NO")
	                + "): done: return NULL");
		return null;
	}
  
  static private boolean isExternalFile(IPath path) {
		if (JavaModelManager.getJavaModelManager().isExternalFile(path)) {
			return true;
		}
		if (JavaModelManager.ZIP_ACCESS_VERBOSE) {
			System.out.println("(" + Thread.currentThread() + ") [JavaModel.isExternalFile(...)] Checking existence of " + path.toString()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		boolean isFile = path.toFile().isFile();
		if (isFile) {
			JavaModelManager.getJavaModelManager().addExternalFile(path);
		}
		return isFile;
	}
  
  public static IResource getWorkspaceTarget(IPath path) {
		if (path == null || path.getDevice() != null)
			return null;
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		if (workspace == null)
			return null;
		return workspace.getRoot().findMember(path);
	}
  
  private static List<PackageFragment> toPackageFragments(
      IPackageFragmentRoot packageFragmentRoot, String projectUri) throws CoreException {
    IJavaElement[] children = packageFragmentRoot.getChildren();
    if (children == null) {
      JavaLanguageServerPlugin.logInfo(
          ">>> \tGetDestinationsCommand.toPackageFragments("
              + packageFragmentRoot.getElementName()
              + "): empty children");
      return null;
    }
    List<PackageFragment> result = new ArrayList<>();
    for (IJavaElement child : children) {
      if (child instanceof IPackageFragment) {
        IPackageFragment packageFragment = (IPackageFragment) child;
        PackageFragment fragment = new PackageFragment();
        fragment.setName(packageFragment.getElementName());
        fragment.setUri(JDTUtils.getFileURI(packageFragment.getResource()));
        fragment.setProjectUri(projectUri);
        result.add(fragment);
      }
    }
    JavaLanguageServerPlugin.logInfo(
        ">>> \tGetDestinationsCommand.toPackageFragments("
            + packageFragmentRoot.getElementName()
            + "): has "
            + result.size()
            + " package fragments");
    return result;
  }
}
