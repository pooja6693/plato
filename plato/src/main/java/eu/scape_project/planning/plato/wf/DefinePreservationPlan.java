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
/**
 * 
 */
package eu.scape_project.planning.plato.wf;

import javax.ejb.Stateful;
import javax.enterprise.context.ConversationScoped;

import eu.scape_project.planning.model.PlanState;

/**
 * Class containing business logic for workflow-step Define Preservation Plan.
 * 
 * @author Markus Hamm, Michael Kraxner
 */
@Stateful
@ConversationScoped
public class DefinePreservationPlan extends AbstractWorkflowStep {
	private static final long serialVersionUID = -1393530549080374281L;


	public DefinePreservationPlan() {
    	requiredPlanState = PlanState.EXECUTEABLE_PLAN_CREATED;
    	correspondingPlanState = PlanState.PLAN_DEFINED;
	}
	
	@Override
	protected void saveStepSpecific() {
		saveEntity(plan.getPlanDefinition());
	}
}