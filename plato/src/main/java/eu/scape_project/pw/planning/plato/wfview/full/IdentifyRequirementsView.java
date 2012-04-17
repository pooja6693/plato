/**
 * 
 */
package eu.scape_project.pw.planning.plato.wfview.full;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;

import org.richfaces.event.FileUploadEvent;
import org.richfaces.model.UploadedFile;
import org.slf4j.Logger;

import eu.planets_project.pp.plato.model.DigitalObject;
import eu.planets_project.pp.plato.model.Plan;
import eu.planets_project.pp.plato.model.PlanState;
import eu.planets_project.pp.plato.model.scales.BooleanScale;
import eu.planets_project.pp.plato.model.scales.FloatRangeScale;
import eu.planets_project.pp.plato.model.scales.FloatScale;
import eu.planets_project.pp.plato.model.scales.FreeStringScale;
import eu.planets_project.pp.plato.model.scales.IntRangeScale;
import eu.planets_project.pp.plato.model.scales.IntegerScale;
import eu.planets_project.pp.plato.model.scales.OrdinalScale;
import eu.planets_project.pp.plato.model.scales.PositiveFloatScale;
import eu.planets_project.pp.plato.model.scales.PositiveIntegerScale;
import eu.planets_project.pp.plato.model.scales.Scale;
import eu.planets_project.pp.plato.model.scales.YanScale;
import eu.planets_project.pp.plato.model.tree.Leaf;
import eu.planets_project.pp.plato.model.tree.Node;
import eu.planets_project.pp.plato.model.tree.TreeNode;
import eu.scape_project.pw.planning.manager.StorageException;
import eu.scape_project.pw.planning.plato.bean.TreeHelperBean;
import eu.scape_project.pw.planning.plato.wf.AbstractWorkflowStep;
import eu.scape_project.pw.planning.plato.wf.IdentifyRequirements;
import eu.scape_project.pw.planning.plato.wfview.AbstractView;
import eu.scape_project.pw.planning.plato.wfview.beans.CriterionSelector;
import eu.scape_project.pw.planning.utils.Downloader;

/**
 * @author Michael Kraxner, Markus Hamm
 */
@Named("identifyRequirements")
@ConversationScoped
public class IdentifyRequirementsView extends AbstractView {

	private static final long serialVersionUID = -1209024050675908703L;
	
	@Inject private Logger log;
	
	@Inject private IdentifyRequirements identifyRequirements;
	
	@Inject private Downloader downloader;
	
	/**
	 * Supporting class for AJAX Criterion selection. This selection is used for requirement - criterion mapping.
	 */
	@Inject	private CriterionSelector critSelector;
	
	@Inject private TreeHelperBean requirementstreeHelper;
	
    /**
     * Indicates whether the nodes comments are edited or scale,restriction, unit ...
     */
	private boolean editingNodeComments = false;
	
	/**
	 * Variable encapsulating the ObjectiveTree-Root in a list.
	 * This is required, because <rich:treeModelRecursiveAdaptor> root variable requires a list to work properly. 
	 */
	private List<TreeNode> treeRoots;
	
	/**
	 * Items for the scale SelectMenu.
	 */
	private List<SelectItem> scaleList;
	
	private DigitalObject importFile;
	
	/**
	 * Leaf selected (last clicked) in the requirements-tree.
	 */
	private Leaf selectedLeaf;
	
    public IdentifyRequirementsView() {
    	currentPlanState = PlanState.RECORDS_CHOSEN;
    	name = "Identify Requirements";
    	viewUrl = "/plan/identifyrequirements.jsf";
    	group = "menu.defineRequirements";
    	
    	treeRoots = new ArrayList<TreeNode>();
    	scaleList = new ArrayList<SelectItem>();
    	importFile = null;
	}    
	
	public void init(Plan plan) {
    	super.init(plan);
    	treeRoots = new ArrayList<TreeNode>();
    	treeRoots.add(plan.getTree().getRoot());
    	populateScaleList();
    	critSelector.init();
    	
    	requirementstreeHelper.expandAll(plan.getTree().getRoot());
	}

    /**
     * Attaches a new Leaf to the given object (which is, hopefully, a Node).
     * 
     * @param object Node where the new Leaf should be attached. 
     */
    public void addLeaf(Object object) {
        if (object instanceof Node) {        	
            Node node = (Node) object;
            identifyRequirements.addNewLeaf(node);            
        }
    }

    /**
     * Attaches a new Node to the given object (which is, hopefully, a Node)
     * 
     * @param object Node where the new Node should be attached.
     */
    public void addNode(Object object) {
        if (object instanceof Node) {
            Node node = (Node) object;
            identifyRequirements.addNewNode(node);
        }
    }
    
    /**
     * Method responsible for removing a given node from the objective tree.
     * 
     * @param objectToRemove Node to remove from the objective tree.
     */
    public void removeTreeNode(Object objectToRemove) {
    	TreeNode nodeToRemove = (TreeNode) objectToRemove;
    	requirementstreeHelper.closeNode(nodeToRemove);
    	identifyRequirements.removeTreeNode(nodeToRemove);
    }
    
    
    /**
     * Converts a node to a leaf.
     * 
     * @param object Node to convert to a leaf.
     */
    public void convertToLeaf(Object object) {
    	identifyRequirements.convertToLeaf((Node) object);
    }
    
    /**
     * Converts a leaf to a node.
     * 
     * @param object Leaf to convert to a node.
     */
    public void convertToNode(Object object) {
    	identifyRequirements.convertToNode((Leaf) object);
    }
    
    /**
     * Method responsible for starting the download of a wanted file.
     * 
     * @param object DigitalObject wanted to download.
     */
    public void downloadAttachedFile(DigitalObject object) {
    	DigitalObject objectIncludingData = null;
    	
		try {
			objectIncludingData = identifyRequirements.fetchAttachedFile(object);
		}
		catch (StorageException e) {
			log.error("Exception at trying to fetch attached file with pid " + object.getPid() + ": " + e.getMessage(), e);
			facesMessages.addError("Unable to fetch attached file");
			return;
		}
    	
    	downloader.download(objectIncludingData);
    }
    
	/**
	 * Method responsible for uploading/attaching a file.
	 * 
	 * @param event Richfaces FileUploadEvent data.
	 */
    public void uploadFile(FileUploadEvent event) {
		UploadedFile file = event.getUploadedFile();
		
		// Put file-data into a digital object
		DigitalObject digitalObject = new DigitalObject();
		digitalObject.setFullname(file.getName());
		digitalObject.getData().setData(file.getData());
		digitalObject.setContentType(file.getContentType());
		
		try {
			identifyRequirements.attachFile(digitalObject);
		} catch (StorageException e) {
			log.error("Exception at trying to attach file. ", e);
			facesMessages.addError("Unable to attach file");
		}
    }
    
    /**
     * Method responsible for removing a previously attached file.
     * 
     * @param fileToRemove Attached file to remove.
     */
    public void removeAttachedFile(DigitalObject fileToRemove) {
    	identifyRequirements.removeAttachedFile(fileToRemove);
    }
    
    /**
     * Method responsible for selecting/setting a file for a later import.
     * 
     * @param event Richfaces FileUploadEvent data.
     */
    public void selectImportFile(FileUploadEvent event) {
    	UploadedFile file = event.getUploadedFile();
    	
    	// Do some input checks
    	if (!file.getName().endsWith("mm")) {
    		facesMessages.addError("importPanel", "Please select a FreeMind file.");
    		importFile = null;
    		return;
    	}
    	
		// Put file-data into a digital object
		importFile = new DigitalObject();
		importFile.setFullname(file.getName());
		importFile.getData().setData(file.getData());
		importFile.setContentType(file.getContentType());
    }
    
    /**
     * Method responsible for importing a FreeMind file which does not contain units.
     */
    public void importFreeMindWithoutUnits() {
    	boolean success = identifyRequirements.importTreeFromFreeMind(importFile, false);
    	
    	if (success) {
    		facesMessages.addInfo("importPanel", "Tree imported successfully");
    		importFile = null;
    		// reset tree root of objective tree
    		treeRoots.clear();
    		treeRoots.add(plan.getTree().getRoot());
        	requirementstreeHelper.expandAll(plan.getTree().getRoot());
    	}
    	else {
    		facesMessages.addError("importPanel", "Error at importing FreeMind file. Maybe it is currupted. " +
    							   "Please make sure you added at least one level of nodes to the midmap.");
    	}
    }
    
    /**
     * Method responsible for importing a FreeMind file which does contain units.
     */
    public void importFreeMindWithUnits() {
    	boolean success = identifyRequirements.importTreeFromFreeMind(importFile, true);
    	
    	if (success) {
    		facesMessages.addInfo("importPanel", "Tree imported successfully");
    		importFile = null;
    		// reset tree root of objective tree
    		treeRoots.clear();
    		treeRoots.add(plan.getTree().getRoot());
    		requirementstreeHelper.expandAll(plan.getTree().getRoot());
    	}
    	else {
    		facesMessages.addError("importPanel",  "Error at importing FreeMind file. Maybe it is currupted. " +
    							   "Please make sure you added at least one level of nodes to the midmap.");
    	}
    }
    
    /**
     * Method responsible for starting the download of the current requirement tree represented in FreeMind format.
     */
    public void downloadTreeAsFreeMind() {
    	String freeMindXML = identifyRequirements.exportTreeAsFreeMindXML();
    	downloader.downloadMM(freeMindXML, plan.getPlanProperties().getName() + ".mm");
    }
   
    /**
     * Method responsible for populating possible SelectItmes of the Scale SelectBox (showing up in the requirements tree).
     */
    private void populateScaleList() {
        if (scaleList.size() > 0) {
            return;
        }

        List<Scale> scales = new ArrayList<Scale>();
        scales.add(new BooleanScale());
        scales.add(new YanScale());
        scales.add(new OrdinalScale());
        scales.add(new IntegerScale());
        scales.add(new IntRangeScale());
        scales.add(new FloatScale());
        scales.add(new FloatRangeScale());
        scales.add(new PositiveFloatScale());
        scales.add(new PositiveIntegerScale());
        scales.add(new FreeStringScale());

        for (Scale scale : scales) {
        	scaleList.add(new SelectItem(scale.getClass().getCanonicalName(), scale.getDisplayName()));
        }
	}
    
    /**
     * Method responsible for setting the selected (last clicked) leaf.
     * 
     * @param leaf Selected leaf 
     */
    public void selectLeaf(Object leaf) {
        if (!(leaf instanceof Leaf))
            return;
        selectedLeaf = (Leaf) leaf;       
        log.debug("Selected leaf with id=" + selectedLeaf.getId());
    }
    
    /**
     * Method responsible for attaching the user selected criterion to the selected leaf.
     */
    public void assignCriterionMapping() {
        identifyRequirements.assignCriterionToLeaf(critSelector.getSelectedCriterion(), selectedLeaf);    	
    }

    /**
     * Method responsible for detaching any criterion from the selected leaf.
     */
    public void detachCriterionMapping() {
    	identifyRequirements.detachCriterionFromLeaf(selectedLeaf);
    }
    
	@Override
	protected AbstractWorkflowStep getWfStep() {
		return identifyRequirements;
	}
	
	// --------------- getter/setter ---------------
	
    public boolean isEditingNodeComments() {
		return editingNodeComments;
	}

	public void setEditingNodeComments(boolean editingNodeComments) {
		this.editingNodeComments = editingNodeComments;
	}

	public List<TreeNode> getTreeRoots() {
		return treeRoots;
	}

	public void setTreeRoots(List<TreeNode> treeRoots) {
		this.treeRoots = treeRoots;
	}

	public List<SelectItem> getScaleList() {
		return scaleList;
	}

	public void setScaleList(List<SelectItem> scaleList) {
		this.scaleList = scaleList;
	}

	public DigitalObject getImportFile() {
		return importFile;
	}

	public void setImportFile(DigitalObject importFile) {
		this.importFile = importFile;
	}

	public CriterionSelector getCritSelector() {
		return critSelector;
	}

	public void setCritSelector(CriterionSelector critSelector) {
		this.critSelector = critSelector;
	}

	public TreeHelperBean getRequirementstreeHelper() {
		return requirementstreeHelper;
	}	
}
