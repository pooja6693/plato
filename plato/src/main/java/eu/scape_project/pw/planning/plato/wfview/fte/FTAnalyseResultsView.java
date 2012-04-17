package eu.scape_project.pw.planning.plato.wfview.fte;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

import eu.planets_project.pp.plato.model.Alternative;
import eu.planets_project.pp.plato.model.Plan;
import eu.planets_project.pp.plato.model.PlanState;
import eu.planets_project.pp.plato.model.SampleObject;
import eu.planets_project.pp.plato.model.beans.ResultNode;
import eu.planets_project.pp.plato.model.tree.TreeNode;
import eu.scape_project.pw.planning.manager.StorageException;
import eu.scape_project.pw.planning.plato.bean.TreeHelperBean;
import eu.scape_project.pw.planning.plato.fte.FTAnalyseResults;
import eu.scape_project.pw.planning.plato.wf.AbstractWorkflowStep;
import eu.scape_project.pw.planning.plato.wfview.AbstractView;
import eu.scape_project.pw.planning.plato.wfview.ViewWorkflowManager;
import eu.scape_project.pw.planning.plato.wfview.beans.ReportLeaf;
import eu.scape_project.pw.planning.utils.Downloader;

@Named("ftAnalyseResults")
@ConversationScoped
public class FTAnalyseResultsView extends AbstractView {
	private static final long serialVersionUID = 1L;

	@Inject private Logger log;
	
	@Inject private Downloader downloader;
	
	@Inject private FTAnalyseResults ftAnalyseResults;
	
	@Inject private ViewWorkflowManager viewWorkflowManager;

	@Inject private TreeHelperBean treeHelper;
	
	
	/**
	 * Variable encapsulating the RequirementsTree-Root in a list.
	 * This is required, because <rich:treeModelRecursiveAdaptor> root variable requires a list to work properly. 
	 */
	private List<TreeNode> requirementsRoots;
	
	/**
	 * List of leaves containing result- and transformed-values
	 */
	private List<ReportLeaf> leafBeans;

	/**
	 * Variable encapsulating the aggregated multiplication result tree-Root in a list.
	 * This is required, because <rich:treeModelRecursiveAdaptor> root variable requires a list to work properly.
	 */
	private List<ResultNode> aggMultResultNodes;

	/**
	 * Variable encapsulating the aggregated sum result tree-Root in a list.
	 * This is required, because <rich:treeModelRecursiveAdaptor> root variable requires a list to work properly.
	 */
	private List<ResultNode> aggSumResultNodes;
	
	/**
	 * Indicates if all considered alternatives should be shown in the weighted sum result tree.
	 */
	private boolean showAllConsideredAlternativesForWeightedSum;
	
	/**
	 * Alternatives showed in weighted sum result tree.
	 */
	private List<Alternative> weightedSumResultTreeShownAlternatives;
	
	/**
	 * Alternatives which did not produce a knock-out(=evaluate to 0) and therefore can be choosen as recommened one. 
	 */
	private List<Alternative> acceptableAlternatives;

	/**
	 * Recommended alternative selected by the user represented as String (Because JSF-SelectOneMenu cannot handle Strings appropriately).
	 */
	private String recommendedAlternativeAsString;
	
	
	public FTAnalyseResultsView() {
    	currentPlanState = PlanState.FTE_ALTERNATIVES_EVALUATED;
    	name = "Analyse Results";
    	viewUrl = "/fte/FTanalyseresults.jsf";
    	requirementsRoots = new ArrayList<TreeNode>();
    	leafBeans = new ArrayList<ReportLeaf>();
		aggSumResultNodes = new ArrayList<ResultNode>();
		aggMultResultNodes = new ArrayList<ResultNode>();
		showAllConsideredAlternativesForWeightedSum = false;
		weightedSumResultTreeShownAlternatives = new ArrayList<Alternative>();
		acceptableAlternatives = new ArrayList<Alternative>();
		recommendedAlternativeAsString = "";
	}
	
	@Override
	public void init(Plan plan) {
		super.init(plan);
		
    	requirementsRoots.clear();
    	if (plan.getTree() != null) {
    		requirementsRoots.add(plan.getTree().getRoot());
    	}
    	
    	leafBeans = ftAnalyseResults.constructPlanReportLeaves();
    	
    	aggMultResultNodes.clear();
    	aggMultResultNodes.add(ftAnalyseResults.getAggregatedMultiplicationResultNode());
    	aggSumResultNodes.clear();
    	aggSumResultNodes.add(ftAnalyseResults.getAggregatedSumResultNode());
    	
    	acceptableAlternatives = ftAnalyseResults.getAcceptableAlternatives();
    	
    	showAllConsideredAlternativesForWeightedSum = false;
    	weightedSumResultTreeShownAlternatives = acceptableAlternatives;
    	
    	if (plan.getRecommendation().getAlternative() != null) {
    		recommendedAlternativeAsString = plan.getRecommendation().getAlternative().getName();
    	}
    	else {
    		recommendedAlternativeAsString = "";
    	}
		// all leaves are shown, unless the users decided to change this. 
		if (treeHelper.getExpandedNodes().isEmpty()) {
			treeHelper.expandAll(plan.getTree().getRoot());
		}
    	
	}

	/**
	 * Method responsible for returning the current time as String.
	 * 
	 * @return Current time as String.
	 */
	public String getCurrentDate() {
		return SimpleDateFormat.getDateTimeInstance().format(new Date());
	}
	
	/**
	 * Method responsible for starting the download for the given sample object.
	 * 
	 * @param sample SampleObject to download.
	 */
	public void downloadSample(Object object) {
		SampleObject sample = (SampleObject) object;
		
		try {
			downloader.download(ftAnalyseResults.fetchDigitalObject(sample));
		} catch (StorageException e) {
			log.error("Exception at trying to fetch sample file with pid " + sample.getPid(), e);
			return;
		}
	}
	
	/**
	 * Method responsible for switching listed weighted sum alternatives between all considered and all acceptable.
	 */
	public void switchShowAllConsideredAlternativesForWeightedSum() {
		if (showAllConsideredAlternativesForWeightedSum) {
			showAllConsideredAlternativesForWeightedSum = false;
			weightedSumResultTreeShownAlternatives = acceptableAlternatives;
		}
		else {
			showAllConsideredAlternativesForWeightedSum = true;
			weightedSumResultTreeShownAlternatives = plan.getAlternativesDefinition().getConsideredAlternatives();
		}
	}
	
	/**
	 * Method responsible for updating the recommended alternative in the model based on the given alternative-name. 
	 * 
	 * @param alternativeName Alternative name of the recommended alternative
	 */
	private void updateAlternativeRecommendation(String alternativeName) {
		Alternative recommendedAlternative = null;
		
		for (Alternative a : plan.getAlternativesDefinition().getAlternatives()) {
			if (a.getName().equals(alternativeName)) {
				recommendedAlternative = a;
				break;
			}
		}
		
		plan.getRecommendation().setAlternative(recommendedAlternative);
	}
	
	/**
	 * Method responsible for converting this fast track workflow into a standard workflow
	 * and continuing it as those.
	 * @return outcome-string of the first page (from standard workflow) to render 
	 */
	public String continueAsStandardPreservationPlan() {
		// transform the plan
		ftAnalyseResults.transformToStandardPreservationPlan();
				
		// restart the workflow to be assembled by standard workflow-steps
		viewWorkflowManager.endWorkflow();
		
		return viewWorkflowManager.startWorkflow(plan);
	}
	
	/**
	 * This is the end of the fast track workflow - so no proceed is possible here.
	 * End-status of the plan is set in {@link FTAnalyseResults#saveStepSpecific()} if a recommendation is set.
	 * This fast track plan can also be continued as standard plan after calling {@link FTAnalyseResults#transformToStandardPreservationPlan()} 
	 */
	@Override
	public String proceed() {
		return null;
	}
	
	@Override
	protected AbstractWorkflowStep getWfStep() {
		return ftAnalyseResults;
	}

	// --------------- getter/setter ---------------
	
	public List<TreeNode> getRequirementsRoots() {
		return requirementsRoots;
	}

	public void setRequirementsRoots(List<TreeNode> requirementsRoots) {
		this.requirementsRoots = requirementsRoots;
	}

	public List<ReportLeaf> getLeafBeans() {
		return leafBeans;
	}

	public void setLeafBeans(List<ReportLeaf> leafBeans) {
		this.leafBeans = leafBeans;
	}

	public List<ResultNode> getAggMultResultNodes() {
		return aggMultResultNodes;
	}

	public void setAggMultResultNodes(List<ResultNode> aggMultResultNodes) {
		this.aggMultResultNodes = aggMultResultNodes;
	}

	public List<ResultNode> getAggSumResultNodes() {
		return aggSumResultNodes;
	}

	public void setAggSumResultNodes(List<ResultNode> aggSumResultNodes) {
		this.aggSumResultNodes = aggSumResultNodes;
	}

	public boolean isShowAllConsideredAlternativesForWeightedSum() {
		return showAllConsideredAlternativesForWeightedSum;
	}

	public void setShowAllConsideredAlternativesForWeightedSum(
			boolean showAllConsideredAlternativesForWeightedSum) {
		this.showAllConsideredAlternativesForWeightedSum = showAllConsideredAlternativesForWeightedSum;
	}

	public List<Alternative> getWeightedSumResultTreeShownAlternatives() {
		return weightedSumResultTreeShownAlternatives;
	}

	public void setWeightedSumResultTreeShownAlternatives(
			List<Alternative> weightedSumResultTreeShownAlternatives) {
		this.weightedSumResultTreeShownAlternatives = weightedSumResultTreeShownAlternatives;
	}

	public List<Alternative> getAcceptableAlternatives() {
		return acceptableAlternatives;
	}

	public void setAcceptableAlternatives(List<Alternative> acceptableAlternatives) {
		this.acceptableAlternatives = acceptableAlternatives;
	}

	public String getRecommendedAlternativeAsString() {
		return recommendedAlternativeAsString;
	}

	public void setRecommendedAlternativeAsString(
			String recommendedAlternativeAsString) {
		this.recommendedAlternativeAsString = recommendedAlternativeAsString;
		updateAlternativeRecommendation(recommendedAlternativeAsString);
	}

	public TreeHelperBean getTreeHelper() {
		return treeHelper;
	}
}
