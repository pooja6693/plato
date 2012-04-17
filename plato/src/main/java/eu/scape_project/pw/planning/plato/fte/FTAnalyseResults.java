package eu.scape_project.pw.planning.plato.fte;

import java.util.List;

import javax.ejb.Stateful;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.planets_project.pp.plato.model.Alternative;
import eu.planets_project.pp.plato.model.Plan;
import eu.planets_project.pp.plato.model.PlanState;
import eu.planets_project.pp.plato.model.beans.ResultNode;
import eu.planets_project.pp.plato.model.tree.Leaf;
import eu.scape_project.pw.planning.plato.wf.AbstractWorkflowStep;
import eu.scape_project.pw.planning.plato.wf.AnalyseResults;
import eu.scape_project.pw.planning.plato.wfview.beans.ReportLeaf;

/**
 * @author Markus Hamm
 */
@Stateful
@ConversationScoped
public class FTAnalyseResults extends AbstractWorkflowStep {
	private static final long serialVersionUID = 6356595757974848875L;

	@Inject private Logger log;
	
	// BL of full wf-steps
	@Inject AnalyseResults analyseResults;
	
	public FTAnalyseResults() {
		this.requiredPlanState = PlanState.FTE_ALTERNATIVES_EVALUATED;
		this.correspondingPlanState = PlanState.FTE_RESULTS_ANALYSED;
	}
	
	@Override
	public void init(Plan p) {
		super.init(p);
		
		// map unmapped text-transformer values to neutral 2.5
		initTextTransformer(new Double(2.5));
		
		// Init correspoding wf-step BL beans (this is mandatory to be able to call them later)
		analyseResults.init(p);
	}
	
	/**
	 * Method delegating the BL execution to AnalyseResults.
	 * @see AnalyseResults#constructPlanReportLeaves()
	 */
	public List<ReportLeaf> constructPlanReportLeaves() {
		return analyseResults.constructPlanReportLeaves();
	}

	/**
	 * Method delegating the BL execution to AnalyseResults.
	 * @see AnalyseResults#getAggregatedMultiplicationResultNode()
	 */
	public ResultNode getAggregatedMultiplicationResultNode() {
		return analyseResults.getAggregatedMultiplicationResultNode();
	}

	/**
	 * Method delegating the BL execution to AnalyseResults.
	 * @see AnalyseResults#getAggregatedSumResultNode()
	 */
	public ResultNode getAggregatedSumResultNode() {
		return analyseResults.getAggregatedSumResultNode();
	}

	/**
	 * Method delegating the BL execution to AnalyseResults.
	 * @see AnalyseResults#getAcceptableAlternatives()
	 */
	public List<Alternative> getAcceptableAlternatives() {
		return analyseResults.getAcceptableAlternatives();
	}
	
	/**
	 * Method responsible for creating a standard preservation plan out of this
	 * fast track preservation plan.
	 */
	public void transformToStandardPreservationPlan() {
		plan.createPPFromFastTrack();
		plan.getPlanProperties().setState(PlanState.INITIALISED);
	}
	
	@Override
	protected void saveStepSpecific() {
		analyseResults.saveWithoutModifyingPlanState();
		
		// set plan state to the completed one if an alternative is given by the user
		// because this last fasttrack step it supports no proceed. Thus if an alternative is set - it is done.
		if (plan.getRecommendation().getAlternative() != null) {
			plan.getPlanProperties().setState(this.correspondingPlanState);
		}
		saveEntity(plan.getPlanProperties());
	}
	
	/**
	 * Method responsible for initializing free text transformer values
	 * AND to map unmapped values to the given unmappedResultValue.
	 * (This is necessary because in fast track evaluation the user is not able to edit the transformer mappings by himself.
	 * 
	 * @param unmappedResultValue
	 */
	private void initTextTransformer(Double unmappedResultValue) {
		for (Leaf l : plan.getTree().getRoot().getAllLeaves()) {
			l.initTransformer(unmappedResultValue);
		}
	}
}
