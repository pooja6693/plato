/*******************************************************************************
 * Copyright 2006 - 2012 Vienna University of Technology,
 * Department of Software Technology and Interactive Systems, IFS
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package eu.scape_project.planning.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import eu.scape_project.planning.model.Alternative;
import eu.scape_project.planning.model.Decision;
import eu.scape_project.planning.model.Plan;
import eu.scape_project.planning.model.PlanState;
import eu.scape_project.planning.model.SampleObject;
import eu.scape_project.planning.model.tree.TreeNode;

/**
 * Validates a plan against a plan state.
 * 
 * @author Michael Kraxner
 */
public class PlanValidator implements Serializable {
    private static final long serialVersionUID = -1592023039267764507L;

    @Inject
    TreeValidator treeValidator;

    public PlanValidator() {
    }

    /**
     * Checks if the plan contains all information required for the given state.
     * - Does
     * <p>
     * not
     * </p>
     * validate the preceding plan states.
     * 
     * @param plan
     * @param state
     * @return
     */
    public boolean isPlanStateSatisfied(Plan plan, PlanState state, List<ValidationError> errors) {
        boolean result = true;

        // if the plan
//        if (plan.getPlanProperties().getState().getValue() < (state.getValue() - 1)) {
//            return false;
//        }
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        
        switch (state) {
            case BASIS_DEFINED:
                result = isBasisDefinedSatisfied(plan, errors);
                break;
            case RECORDS_CHOSEN:
                result = isRecordsChosenSatisfied(plan, errors);
                break;
            case TREE_DEFINED:
                result = isRequirementsDefinedSatisfied(plan, errors);
                break;
            case ALTERNATIVES_DEFINED:
                result = isAlternativesDefinedSatisfied(plan, errors);
                break;
            case GO_CHOSEN:
                result = isDecisionChosenSatisfied(plan, errors);
                break;
            case EXPERIMENT_DEFINED:
                result = isExperimentDefinedSatisfied(plan, errors);
                break;
            case EXPERIMENT_PERFORMED:
                result = isExperimentPerformedSatisfied(plan, errors);
                break;
            case RESULTS_CAPTURED:
                result = isResultsCapturedSatisfied(plan, errors);
                break;
            case TRANSFORMATION_DEFINED:
                result = isTransformationDefinedSatisfied(plan, errors);
                break;
            case WEIGHTS_SET:
                result = isWeightsSetSatisfied(plan, errors);
                break;
            case ANALYSED:
                result = isAnalysedSatisfied(plan, errors);
                break;
            case EXECUTEABLE_PLAN_CREATED:
                result = isExecutablePlanCreatedSatisfied(plan, errors);
                break;
            case PLAN_DEFINED:
                result = isPlanDefinedSatisfied(plan, errors);
                break;
            case PLAN_VALIDATED:
                result = isPlanValidatedSatisfied(plan, errors);
                break;
            default:
                break;
        }
        return result;
    }

    private boolean isDecisionChosenSatisfied(Plan plan, List<ValidationError> errors) {

        boolean result = true;
        Decision decision = plan.getDecision();

        if (!decision.isGoDecision()) {
            result = false;
            errors.add(new ValidationError("You have to take the GO decision to proceed with the workflow."));
        }
        if (plan.getAlternativesDefinition().getConsideredAlternatives().size() == 0) {
            errors.add(new ValidationError("At least one alternative must be considered for evaluation."));
            result = false;
        }
        // FIXME add validation of required fields, - javax - validation
        // constraints

        return result;
    }

    private boolean isExperimentDefinedSatisfied(Plan plan, List<ValidationError> errors) {
        // TODO Auto-generated method stub
        // FIXME add validation of required fields, - javax - validation
        // constraints

        return true;
    }

    private boolean isExperimentPerformedSatisfied(Plan plan, List<ValidationError> errors) {
        return true;
    }

    private boolean isResultsCapturedSatisfied(Plan plan, List<ValidationError> errors) {
        // FIXME add validation of required fields, - javax - validation
        // constraints

        // we do not want to define a specific NodeValidator, therefore we
        // create an anonymous class
        // we have to make this plan available as final variable, so the
        // anonymous class can access it
        final Plan thisPlan = plan;

        boolean result = treeValidator.validate(plan.getTree().getRoot(), new INodeValidator() {
            private List<Alternative> consideredAlternatives = thisPlan.getAlternativesDefinition()
                .getConsideredAlternatives();

            public boolean validateNode(TreeNode node, List<ValidationError> errors) {
                return node.isCompletelyEvaluated(consideredAlternatives, errors);
            }
        }, errors);

        return result;
    }

    private boolean isTransformationDefinedSatisfied(Plan plan, List<ValidationError> errors) {
        boolean result = treeValidator.validate(plan.getTree().getRoot(), new INodeValidator() {
            public boolean validateNode(TreeNode node, List<ValidationError> errors) {
                return node.isCompletelyTransformed(errors);
            }
        }, errors);

        return result;
    }

    private boolean isWeightsSetSatisfied(Plan plan, List<ValidationError> errors) {
        boolean result = treeValidator.validate(plan.getTree().getRoot(), new INodeValidator() {
            public boolean validateNode(TreeNode node, List<ValidationError> errors) {
                return node.isCorrectlyWeighted(errors);
            }
        }, errors);

        return result;
    }

    private boolean isAnalysedSatisfied(Plan plan, List<ValidationError> errors) {
        // if no recommendation is set - validation fails
        if (plan.getRecommendation().getAlternative() == null) {
            errors.add(new ValidationError("You have to select a recommendation to proceed with the workflow."));
            return false;
        }

        return true;
    }

    private boolean isExecutablePlanCreatedSatisfied(Plan plan, List<ValidationError> errors) {
        // no validation at this step
        return true;
    }

    private boolean isPlanDefinedSatisfied(Plan plan, List<ValidationError> errors) {
        // no validation at this step
        return true;
    }

    private boolean isPlanValidatedSatisfied(Plan plan, List<ValidationError> errors) {
        // no validation at this step
        return true;
    }

    private boolean isAlternativesDefinedSatisfied(Plan plan, List<ValidationError> errors) {
        // FIXME add validation of required fields, - javax - validation
        // constraints

        // at least one alternative must be defined
        if (plan.getAlternativesDefinition().getAlternatives().size() <= 0) {
            ValidationError error = new ValidationError(
                "At least one alternative must be added to proceed with the workflow.");
            errors.add(error);
            return false;
        }

        return true;
    }

    private boolean isRequirementsDefinedSatisfied(Plan plan, List<ValidationError> errors) {
        boolean result = treeValidator.validate(plan.getTree().getRoot(), new INodeValidator() {
            public boolean validateNode(TreeNode node, List<ValidationError> errors) {
                return node.isCompletelySpecified(errors);
            }
        }, errors);

        return result;
    }

    private boolean isRecordsChosenSatisfied(Plan plan, List<ValidationError> errors) {
        boolean result = true;
        // at least one sample must be defined
        if (plan.getSampleRecordsDefinition().getRecords().size() == 0) {
            result = false;
            errors.add(new ValidationError("At least one sample must be added to proceed with the workflow."));
        }
        // sample names must be unique
        List<String> names = new ArrayList<String>();
        for (SampleObject sample : plan.getSampleRecordsDefinition().getRecords()) {
            if (names.contains(sample.getShortName())) {
                result = false;
                errors.add(new ValidationError("There are two samples with the same short name '"
                    + sample.getShortName() + "'. Please provide unique names."));
            }
            names.add(sample.getShortName());
        }
        // FIXME add validation of required fields, - javax - validation
        // constraints

        return result;
    }

    private boolean isBasisDefinedSatisfied(Plan plan, List<ValidationError> errors) {
        // FIXME add validation of required fields, - javax - validation
        // constraints
        return true;
    }
}
