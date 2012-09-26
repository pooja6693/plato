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
package eu.scape_project.planning.plato.wfview.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import eu.scape_project.planning.manager.CriteriaManager;
import eu.scape_project.planning.model.measurement.Attribute;
import eu.scape_project.planning.model.measurement.CriterionCategory;
import eu.scape_project.planning.model.measurement.Measure;

/**
 * Class responsible as supporting class for AJAX Criterion selection.
 * 
 * @author Markus Hamm
 */
public class CriterionSelector implements Serializable {
    private static final long serialVersionUID = -4868553688311289177L;

    @Inject
    private CriteriaManager criteriaManager;

    private Collection<CriterionCategory> categories;

    private List<Attribute> allAttributes;
    private Collection<Attribute> filteredAttributes;

    private List<Measure> allMeasures;
    private Collection<Measure> filteredMeasures;

    private CriterionCategory selectedCategory;
    private Attribute selectedAttribute;
    private Measure selectedMeasure;

    /**
     * Compares two Attribute instances regarding their name
     * 
     * @author kraxner
     * 
     */
    class AttributeNameComparator implements Comparator<Attribute> {
        @Override
        public int compare(Attribute o1, Attribute o2) {
            if (null == o1) {
                return -1;
            } else if (null == o2) {
                return 1;
            }
            return o1.getName().compareTo(o2.getName());
        }
    }

    class MeasureNameComparator implements Comparator<Measure> {
        @Override
        public int compare(Measure o1, Measure o2) {
            if (null == o1) {
                return -1;
            } else if (null == o2) {
                return 1;
            }
            return o1.getName().compareTo(o2.getName());
        }
    }

    public CriterionSelector() {
        categories = new ArrayList<CriterionCategory>(Arrays.asList(CriterionCategory.values()));

        clearSelection();
    }

    public void init() {
        allAttributes = new ArrayList<Attribute>(criteriaManager.getAllAttributes());
        Collections.sort(allAttributes, new AttributeNameComparator());

        allMeasures = new ArrayList<Measure>(criteriaManager.getAllMeasures());
        Collections.sort(allMeasures, new MeasureNameComparator());

        clearSelection();
    }

    private void clearSelection() {
        filteredAttributes = new ArrayList<Attribute>();
        filteredMeasures = new ArrayList<Measure>();

        selectedCategory = null;
        selectedAttribute = null;
        selectedMeasure = null;
    }

    public void setSelectedAttributeName(String name) {
        selectedAttribute = findAttributeByName(name);
    }

    private Attribute findAttributeByName(String name) {
        if (name == null) {
            return null;
        }
        for (Attribute a : allAttributes) {
            if (name.equals(a.getName())) {
                return a;
            }
        }
        return null;
    }

    private Measure findMeasureByName(String name) {
        if (name == null) {
            return null;
        }
        for (Measure a : allMeasures) {
            if (name.equals(a.getName())) {
                return a;
            }
        }
        return null;
    }

    public String getSelectedAttributeName() {
        if (selectedAttribute == null) {
            return null;
        } else {
            return selectedAttribute.getName();
        }
    }

    public void setSelectedMeasureName(String name) {
        selectedMeasure = findMeasureByName(name);
    }

    public String getSelectedMeasureName() {
        if (selectedMeasure == null) {
            return null;
        } else {
            return selectedMeasure.getName();
        }
    }

    public void applyFilter() {
        filteredAttributes.clear();
        filteredMeasures.clear();

        if (selectedCategory == null) {
            selectedAttribute = null;
            filteredAttributes.addAll(allAttributes);
        } else {
            boolean isSelectedAttributeInList = false;
            for (Attribute attr : allAttributes) {
                if (attr.getCategory() == selectedCategory) {
                    filteredAttributes.add(attr);
    
                    // try to restore previous attribute selection  
                    if ((selectedAttribute != null) && attr.getUri().equals(selectedAttribute.getUri())) {
                        isSelectedAttributeInList = true;
                    }
                }
            }
            // a different category was chosen, the selected attribute has to change
            if (!isSelectedAttributeInList) {
                selectedAttribute = null;
            }
        }
        // if there is only one attribute, preselect it
        if (filteredAttributes.size() == 1) {
            selectedAttribute = filteredAttributes.iterator().next();
        }

        // filter measures
        Measure oldSelectedMeasure = selectedMeasure;
        selectedMeasure = null;
        if ((selectedCategory == null) && (selectedAttribute == null)) {
            filteredMeasures.addAll(allMeasures);
        } else if (selectedAttribute != null){
            for (Measure meas : allMeasures ) {
                if (meas.getAttribute().getUri().equals(selectedAttribute.getUri())) {
                    filteredMeasures.add(meas);
                    if ((oldSelectedMeasure != null) && meas.getUri().equals(oldSelectedMeasure.getUri())) {
                        selectedMeasure = oldSelectedMeasure;
                    }
                }
            }
        }
        // if there is only one measure, preselect it
        if (filteredMeasures.size() == 1) {
            selectedMeasure = filteredMeasures.iterator().next();
        }
    }

    public Measure getSelectedMeasure() {
        return selectedMeasure;
    }

    // --------------- general getter/setter ---------------

    public CriterionCategory getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(CriterionCategory selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    public Collection<CriterionCategory> getCategories() {
        return categories;
    }

    public Collection<Measure> getFilteredMeasures() {
        return filteredMeasures;
    }

    public Collection<Attribute> getFilteredAttributes() {
        return filteredAttributes;
    }

    public Attribute getSelectedAttribute() {
        return selectedAttribute;
    }
}
