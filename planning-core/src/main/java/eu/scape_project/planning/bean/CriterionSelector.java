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
package eu.scape_project.planning.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;

import eu.scape_project.planning.manager.CriteriaManager;
import eu.scape_project.planning.model.measurement.Attribute;
import eu.scape_project.planning.model.measurement.CriterionCategory;
import eu.scape_project.planning.model.measurement.Measure;

public class CriterionSelector implements Serializable {
    private static final long serialVersionUID = -2370084698959293108L;

    /**
     * Listener for change events of the selector.
     */
    public interface ChangeListener extends EventListener {
        /**
         * Called when a category, attribute or measure is selected.
         */
        void selected();
    }

    @Inject
    private CriteriaManager criteriaManager;

    private List<CriterionCategory> categories;

    private List<Attribute> allAttributes;
    private List<Attribute> filteredAttributes;

    private List<Measure> allMeasures;
    private List<Measure> filteredMeasures;

    private CriterionCategory selectedCategory;
    private Attribute selectedAttribute;
    private Measure selectedMeasure;

    private String searchTerm;

    private List<ChangeListener> changeListeners = new ArrayList<ChangeListener>(1);

    /**
     * Compares two category instances regarding their name.
     */
    class CategoryNameComparator implements Comparator<CriterionCategory> {
        @Override
        public int compare(CriterionCategory o1, CriterionCategory o2) {
            if (null == o1) {
                return -1;
            } else if (null == o2) {
                return 1;
            }
            return o1.getName().compareTo(o2.getName());
        }
    }

    /**
     * Compares two attribute instances regarding their name.
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

    /**
     * Compares two measure instances regarding their name.
     */
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

    /**
     * Creates a new criterion selector object.
     */
    public CriterionSelector() {
        clearSelection();
    }

    /**
     * Initializes the Measure selection. Retrieves all available measures from
     * the {@link #criteriaManager CriteriaManager}
     */
    public void init() {
        allMeasures = new ArrayList<Measure>(criteriaManager.getAllMeasures());

        allAttributes = new ArrayList<Attribute>();
        categories = new ArrayList<CriterionCategory>();
        // show only attributes and categories for which measures exist
        for (Measure m : allMeasures) {
            if (!allAttributes.contains(m.getAttribute())) {
                allAttributes.add(m.getAttribute());
            }
        }
        for (Attribute a : allAttributes) {
            if (!categories.contains(a.getCategory())) {
                categories.add(a.getCategory());
            }
        }
        Collections.sort(allMeasures, new MeasureNameComparator());
        Collections.sort(allAttributes, new AttributeNameComparator());
        Collections.sort(categories, new CategoryNameComparator());

        changeListeners.clear();

        clearSelection();
    }

    /**
     * Removes all measures from the list of available measures where the uris
     * are not in the given set of uris.
     * 
     * @param measureUris
     *            The uris of the measures which shall be available for
     *            selection.
     */
    public void filterCriteria(Set<String> measureUris) {
        HashMap<String, CriterionCategory> filteredCategories = new HashMap<String, CriterionCategory>();
        HashMap<String, Attribute> filteredAttributes = new HashMap<String, Attribute>();
        HashMap<String, Measure> filteredMeasures = new HashMap<String, Measure>();

        for (Measure m : allMeasures) {
            if (measureUris.contains(m.getUri())) {
                filteredMeasures.put(m.getUri(), m);
                filteredAttributes.put(m.getAttribute().getUri(), m.getAttribute());
                filteredCategories.put(m.getAttribute().getCategory().getUri(), m.getAttribute().getCategory());
            }
        }

        categories.clear();
        categories.addAll(filteredCategories.values());
        allAttributes.clear();
        allAttributes.addAll(filteredAttributes.values());
        allMeasures.clear();
        allMeasures.addAll(filteredMeasures.values());
    }

    /**
     * Clears the currently selected attribute, measure and searchTerm.
     */
    private void clearSelection() {
        filteredAttributes = new ArrayList<Attribute>();
        filteredMeasures = new ArrayList<Measure>();

        selectedCategory = null;
        selectedAttribute = null;
        selectedMeasure = null;

        searchTerm = "";
    }

    /**
     * Returns the name of the selected category.
     * 
     * @return the category name or null if no category selected
     */
    public String getSelectedCategoryName() {
        if (selectedCategory == null) {
            return null;
        } else {
            return selectedCategory.getName();
        }
    }

    /**
     * Sets the selected category by name.
     * 
     * @param name
     *            the category name
     */
    public void setSelectedCategoryName(String name) {
        this.selectedCategory = findCategoryByName(name);
    }

    /**
     * Sets the selected attribute by name.
     * 
     * @param name
     *            the attribute name
     */
    public void setSelectedAttributeName(String name) {
        selectedAttribute = findAttributeByName(name);
    }

    /**
     * Finds a category by name.
     * 
     * @param name
     *            the category name
     * @return the category or null if no matching category found
     */
    private CriterionCategory findCategoryByName(String name) {
        if (name == null) {
            return null;
        }
        for (CriterionCategory c : categories) {
            if (name.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }

    /**
     * Finds an attribute by name.
     * 
     * @param name
     *            the attribute name
     * @return the attribute or null if no matching attribute found
     */
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

    /**
     * Finds the measure by name.
     * 
     * @param name
     *            the measure name
     * @return the measure or null if no matching measure found
     */
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

    /**
     * Returns the name of the selected attribute.
     * 
     * @return the attribute name or null if no attribute selected
     */
    public String getSelectedAttributeName() {
        if (selectedAttribute == null) {
            return null;
        } else {
            return selectedAttribute.getName();
        }
    }

    /**
     * Sets the selected measure by name.
     * 
     * @param name
     *            the measure name
     */
    public void setSelectedMeasureName(String name) {
        selectedMeasure = findMeasureByName(name);
    }

    /**
     * Returns the name of the selected measure.
     * 
     * @return the measure name of null if no measure selected
     */
    public String getSelectedMeasureName() {
        if (selectedMeasure == null) {
            return null;
        } else {
            return selectedMeasure.getName();
        }
    }

    /**
     * Searches for measures which comply to the currently set
     * {@link #searchTerm}. To be called when the search term has changed.
     */
    public void updateSearch() {
        String[] terms = searchTerm.split("\\s");
        String pattern = "^";
        for (int i = 0; i < terms.length; i++) {
            // we use
            pattern += "(?=.*" + terms[i] + ")";
        }
        pattern += ".*";
        Pattern searchPattern;
        try {
            searchPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        } catch (IllegalArgumentException e) {
            searchPattern = Pattern.compile(".*", Pattern.CASE_INSENSITIVE);
        }

        Set<Attribute> termFilteredAttributes = new HashSet<Attribute>();

        filteredMeasures.clear();
        for (Measure m : allMeasures) {
            // search in all descriptions at once, this is more what the user
            // expects
            String base = m.getName() + " " + m.getDescription() + " " + m.getAttribute().getName() + " "
                + m.getAttribute().getDescription() + " " + m.getAttribute().getCategory().getName();
            if (searchPattern.matcher(base).matches()) {
                filteredMeasures.add(m);
                termFilteredAttributes.add(m.getAttribute());
            }
        }
        filteredAttributes.clear();
        filteredAttributes.addAll(termFilteredAttributes);
        Collections.sort(filteredMeasures, new MeasureNameComparator());
        Collections.sort(filteredAttributes, new AttributeNameComparator());
    }

    /**
     * Notifies this object that a category was selected.
     */
    public void categorySelected() {
        filteredAttributes.clear();
        if (selectedCategory != null) {
            for (Attribute attr : allAttributes) {
                if (attr.getCategory().getUri().equals(selectedCategory.getUri())) {
                    filteredAttributes.add(attr);
                }
            }
        }
        if (!filteredAttributes.contains(selectedAttribute)) {
            selectedAttribute = null;
        }
        // if there is only one attribute, preselect it
        if (filteredAttributes.size() == 1) {
            selectedAttribute = filteredAttributes.iterator().next();
        }

        // propagate the new selection
        attributeSelected();
    }

    /**
     * Notifies this object that a attribute was selected.
     */
    public void attributeSelected() {
        filteredMeasures.clear();
        if (selectedAttribute != null) {
            for (Measure m : allMeasures) {
                if (m.getAttribute().getUri().equals(selectedAttribute.getUri())) {
                    filteredMeasures.add(m);
                }
            }
            // and also adjust the category, in case the textual filter was used
            if ((selectedCategory == null)
                || (!selectedCategory.getUri().equals(selectedAttribute.getCategory().getUri()))) {
                selectedCategory = selectedAttribute.getCategory();
            }
        }
        if (!filteredMeasures.contains(selectedMeasure)) {
            selectedMeasure = null;
        }
        // if there is only one measure, preselect it
        if (filteredMeasures.size() == 1) {
            selectedMeasure = filteredMeasures.iterator().next();
        }

        setSearchTerm("");

        // propagate the new selection
        measureSelected();
    }

    /**
     * Notifies this object that a measure was selected.
     */
    public void measureSelected() {
        if (selectedMeasure != null) {
            if ((selectedAttribute == null)
                || (!selectedAttribute.getUri().equals(selectedMeasure.getAttribute().getUri()))) {
                selectedAttribute = findAttributeByName(selectedMeasure.getAttribute().getName());
                selectedCategory = findCategoryByName(selectedAttribute.getCategory().getName());
            }
        }

        callChangeListeners();
    }

    /**
     * Calls registered listeners.
     */
    private void callChangeListeners() {
        for (ChangeListener c : changeListeners) {
            c.selected();
        }
    }

    /**
     * Selects the provided measures.
     * 
     * @param measure
     *            the measure to select
     */
    public void selectMeasure(Measure measure) {
        if (measure != null) {
            selectedMeasure = measure;
            measureSelected();
            categorySelected();
            selectedMeasure = measure;
        } else {
            clearSelection();
        }

    }

    /**
     * Adds a change listener to this object.
     * 
     * @param changeListener
     *            the change listener to add
     */
    public void addChangeListener(ChangeListener changeListener) {
        changeListeners.add(changeListener);
    }

    /**
     * Removes a change listener from this object.
     * 
     * @param changeListener
     *            the change listener to remove
     */
    public void removeChangeListener(ChangeListener changeListener) {
        changeListeners.remove(changeListener);
    }

    public Measure getSelectedMeasure() {
        return selectedMeasure;
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

    public CriterionCategory getSelectedCategory() {
        return selectedCategory;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String value) {
        this.searchTerm = value;
    }

}
