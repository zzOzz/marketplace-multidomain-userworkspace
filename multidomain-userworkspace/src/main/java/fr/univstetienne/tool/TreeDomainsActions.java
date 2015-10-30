/**
 *
 */

package fr.univstetienne.tool;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Bean de gestion des domaines dans l'arborescence.
 *
 * @author fmunch
 */
@Name("treeDomainsActions")
@Scope(ScopeType.APPLICATION)
public class TreeDomainsActions implements Serializable {
	private static final long serialVersionUID = -4194490379546587081L;

	private static final Log LOG = LogFactory.getLog(TreeDomainsActions.class);

	@In(create = true)
	private transient CoreSession documentManager;

	/**
	 * Constructeur par défaut.
	 */
	public TreeDomainsActions() {
		super();
	}

	/**
	 * Récupère la liste des domaines accessibles en lecture.
	 *
	 * @return Liste des domaines triés par titre.
	 */
	public List<DocumentModel> getDomains() {
		return documentManager.query("SELECT * FROM Domain WHERE ecm:currentLifeCycleState != 'deleted'  ORDER BY dc:title");
	}
}