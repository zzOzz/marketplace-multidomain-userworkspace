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

import static org.jboss.seam.ScopeType.PAGE;
import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.webapp.tree.DocumentTreeNode;
import org.nuxeo.ecm.webapp.tree.DocumentTreeNodeImpl;
import org.nuxeo.ecm.webapp.tree.TreeManager;
import org.nuxeo.runtime.api.Framework;

@Scope(PAGE)
@Name("reducedTreeActions")
@Install(precedence = APPLICATION)
public class MomReducedTreeActionsBean extends MomTreeActionsBean {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(MomReducedTreeActionsBean.class);

    @Override
    public List<DocumentTreeNode> getTreeRoots() {
        return getTreeRoots(true);
    }

    @Override
    public List<DocumentTreeNode> getUserTreeRoots() {
        return getUserTreeRoots(true);
    }

    /**
     * @since 5.4
     * @return a list containing a DocumentTreeNode for the Root document
     */
    public List<DocumentTreeNode> getRootNode() {
        return getTreeRoots(true, documentManager.getRootDocument());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.nuxeo.ecm.webapp.tree.TreeActionsBean#getTreeRoots(boolean,
     * org.nuxeo.ecm.core.api.DocumentModel, java.lang.String)
     */
    @Override
    protected List<DocumentTreeNode> getTreeRoots(boolean showRoot,
            DocumentModel currentDocument, String treeName) {

        if (treeInvalidator.needsInvalidation()) {
            reset();
            treeInvalidator.invalidationDone();
        }
        List<DocumentTreeNode> currentTree = trees.get(treeName);
        if (currentTree == null) {
            currentTree = new ArrayList<DocumentTreeNode>();
            DocumentModel firstAccessibleParent = null;
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

}
