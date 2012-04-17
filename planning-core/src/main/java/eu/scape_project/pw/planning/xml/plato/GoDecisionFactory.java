/*******************************************************************************
 * Copyright (c) 2006-2010 Vienna University of Technology, 
 * Department of Software Technology and Interactive Systems
 *
 * All rights reserved. This program and the accompanying
 * materials are made available under the terms of the
 * Apache License, Version 2.0 which accompanies
 * this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0 
 *******************************************************************************/

package eu.scape_project.pw.planning.xml.plato;

import org.apache.commons.digester3.AbstractObjectCreationFactory;
import org.xml.sax.Attributes;

import eu.planets_project.pp.plato.model.Decision.GoDecision;

/**
 * Helper class for {@link eu.planets_project.pp.plato.xml.ProjectImporter} to create a GoDecision value of its XML representation. 
 * 
 * @author Michael Kraxner
 *
 */
public class GoDecisionFactory extends AbstractObjectCreationFactory<GoDecision> {

    public GoDecisionFactory() {
    }

    @Override
    public GoDecision createObject(Attributes arg0) throws Exception {
        String decision = (arg0.getValue("value"));
        return GoDecision.valueOf(decision);
    }

}
