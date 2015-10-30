/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *     Maison de l'Orient et de la Méditerranée (http://mom.fr) - override
 *
 * $Id$
 */
package fr.mom.nuxeo.ecm.webapp.tree;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.webapp.tree.DocumentTreeNode;
import org.nuxeo.ecm.webapp.tree.DocumentTreeNodeImpl;
import org.nuxeo.ecm.webapp.tree.TreeActionsBean;
import org.nuxeo.ecm.webapp.tree.TreeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Manages the collaborative workspace and the personal workspace navigation
 * trees. Based on the original TreeActionsBean
 * 
 * @author Florent Soulière
 */
@Scope(CONVERSATION)
@Name("treeActions")
@Install(precedence = APPLICATION)
public class MomTreeActionsBean extends TreeActionsBean {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(MomTreeActionsBean.class);

    public static final String NODE_SELECTED_MARKER = MomTreeActionsBean.class.getName()
            + "_NODE_SELECTED_MARKER";

    protected DocumentModel lastAccessedDocument;

    /**
     * Provides the personal user document tree excluding the root
     * 
     * @return List of document trees
     * @throws ClientException
     *
     * @since 6.0
     */
    public List<DocumentTreeNode> getUserTreeRoots() throws ClientException {
        return getUserTreeRoots(false);
    }

    /**
     * Provides the personal user document tree excluding the root and according
     * to the given tree name
     * 
     * @param treeName name of the tree, which will be prefixed by "user" inside
     *            the method
     * @return List of document trees
     * @throws ClientException
     *
     * @since 6.0
     */
    public List<DocumentTreeNode> getUserTreeRoots(String treeName)
            throws ClientException {
        return getUserTreeRoots(false, treeName);
    }

    /**
     * Provides the personal user document tree
     * 
     * @param showRoot indicates whether to show the root tree node when the
     *            document tree is displayed
     * @param treeName name of the tree, which will be prefixed by "user" inside
     *            the method
     * @return List of document trees
     * @throws ClientException
     *
     * @since 6.0
     */
    protected List<DocumentTreeNode> getUserTreeRoots(boolean showRoot,
            String treeName) throws ClientException {
        return getUserTreeRoots(showRoot,
                navigationContext.getCurrentDocument(), treeName);
    }

    /**
     * Provides the personal user document tree for the current document and the
     * default navigation tree name
     * 
     * @param showRoot indicates whether to show the root tree node when the
     *            document tree is displayed
     * @return List of document trees
     * @throws ClientException
     *
     * @since 6.0
     */
    protected List<DocumentTreeNode> getUserTreeRoots(boolean showRoot)
            throws ClientException {
        return getUserTreeRoots(showRoot,
                navigationContext.getCurrentDocument(),
                DEFAULT_TREE_PLUGIN_NAME);
    }

    /**
     * Provides the personal user document tree for the default navigation tree
     * name
     * 
     * @param showRoot indicates whether to show the root tree node when the
     *            document tree is displayed
     * @param currentDocument points to the current document model
     * @return List of document trees
     * @throws ClientException
     *
     * @since 6.0
     */
    protected List<DocumentTreeNode> getUserTreeRoots(boolean showRoot,
            DocumentModel currentDocument) throws ClientException {
        return getUserTreeRoots(showRoot, currentDocument,
                DEFAULT_TREE_PLUGIN_NAME);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.nuxeo.ecm.webapp.tree.TreeActionsBean#getTreeRoots(boolean,
     * org.nuxeo.ecm.core.api.DocumentModel, java.lang.String)
     */
    protected List<DocumentTreeNode> getTreeRoots(boolean showRoot,
            DocumentModel currentDocument, String treeName)
            throws ClientException {

        if (treeInvalidator.needsInvalidation()) {
            reset();
            treeInvalidator.invalidationDone();
        }
        List<DocumentTreeNode> currentTree = trees.get(treeName);
        if (currentTree == null) {
            currentTree = new ArrayList<DocumentTreeNode>();
            DocumentModel firstAccessibleParent = null;
            if (isUserWorkspace) {
                currentDocument = lastAccessedDocument;
            } else {
                lastAccessedDocument = currentDocument;
            }
            if (currentDocument != null) {
                List<DocumentModel> parents = documentManager.getParentDocuments(currentDocument.getRef());
                if (!parents.isEmpty()) {
                    firstAccessibleParent = parents.get(0);
                } else if (!"Root".equals(currentDocument.getType())
                        && currentDocument.isFolder()) {
                    // default on current doc
                    firstAccessibleParent = currentDocument;
                } else {
                    if (showRoot) {
                        firstAccessibleParent = currentDocument;
                    }
                }
            }
            firstAccessibleParentPath = firstAccessibleParent == null ? null
                    : firstAccessibleParent.getPathAsString();
            if (firstAccessibleParent != null) {
                Filter filter = null;
                Filter leafFilter = null;
                Sorter sorter = null;
                String pageProvider = null;
                try {
                    TreeManager treeManager = Framework.getService(TreeManager.class);
                    filter = treeManager.getFilter(treeName);
                    leafFilter = treeManager.getLeafFilter(treeName);
                    sorter = treeManager.getSorter(treeName);
                    pageProvider = treeManager.getPageProviderName(treeName);
                } catch (Exception e) {
                    log.error("Could not fetch filter or sorter for tree ", e);
                }

                DocumentTreeNode treeRoot = null;
                treeRoot = new DocumentTreeNodeImpl(firstAccessibleParent,
                        filter, leafFilter, sorter, pageProvider);
                currentTree.add(treeRoot);
                log.debug("Tree initialized with document: "
                        + firstAccessibleParent.getId());
            } else {
                log.debug("Could not initialize the navigation tree: no parent"
                        + " found for current document");
            }
            trees.put(treeName, currentTree);
        }
        return trees.get(treeName);
    }

    /**
     * Provides the personal user document tree
     * 
     * @param showRoot indicates whether to show the root tree node when the
     *            document tree is displayed
     * @param currentDocument points to the current document model
     * @param treeName name of the tree, which will be prefixed by "user" inside
     *            the method
     * @return List of document trees
     * @throws ClientException
     *
     * @since 6.0
     */
    protected List<DocumentTreeNode> getUserTreeRoots(boolean showRoot,
            DocumentModel currentDocument, String treeName)
            throws ClientException {

        String userTreeName = "user" + treeName;
        List<DocumentTreeNode> currentTree = trees.get(userTreeName);
        if (currentTree == null) {
            currentTree = new ArrayList<DocumentTreeNode>();
            UserWorkspaceService userWorkspaceService = Framework.getLocalService(UserWorkspaceService.class);
            DocumentModel userWorkspaceModel = userWorkspaceService.getCurrentUserPersonalWorkspace(
                    documentManager, navigationContext.getCurrentDocument());
            if (userWorkspaceModel != null) {
                Filter filter = null;
                Filter leafFilter = null;
                Sorter sorter = null;
                String pageProvider = null;
                try {
                    TreeManager treeManager = Framework.getService(TreeManager.class);
                    filter = treeManager.getFilter(treeName);
                    leafFilter = treeManager.getLeafFilter(treeName);
                    sorter = treeManager.getSorter(treeName);
                    pageProvider = treeManager.getPageProviderName(treeName);
                } catch (Exception e) {
                    log.error("Could not fetch filter or sorter for tree ", e);
                }

                DocumentTreeNode treeRoot = null;
                treeRoot = new DocumentTreeNodeImpl(userWorkspaceModel, filter,
                        leafFilter, sorter, pageProvider);
                currentTree.add(treeRoot);
                log.debug("Tree initialized with document: "
                        + userWorkspaceModel.getId());
            } else {
                log.debug("Could not initialize the navigation tree: no parent"
                        + " found for current document");
            }

            trees.put(userTreeName, currentTree);
        }
        return trees.get(userTreeName);
    }

}
