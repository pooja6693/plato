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
 * 
 * This work originates from the Planets project, co-funded by the European Union under the Sixth Framework Programme.
 ******************************************************************************/
package eu.scape_project.planning.model.sensitivity;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import eu.scape_project.planning.model.Alternative;
import eu.scape_project.planning.model.aggregators.IAggregator;
import eu.scape_project.planning.model.beans.ResultNode;
import eu.scape_project.planning.model.tree.TreeNode;

/**
 * This sensitivity analysis test uses the order of the alternatives to determine which nodes are sensitive.
 * At start (with the normal importance weights) the order of the alternatives is evaluated - which 
 * alternative is currently the best, which is second best etc. This is the "normal order".
 * 
 * After each iteration (changes to the importance weights) the alternatives are evaluated once again. If the order
 * changes (compared to "normal order") then the currently processed node is considered sentitive.
 * 
 * @author Jan Zarnikov
 *
 */
public class OrderChangeTest implements ISensitivityTest {
    
    /**
     * This is just a helper class so we can store the alternatives along with
     * theire evaluated result values in a SortedSet.
     * 
     * @author Jan Zarnikov
     *
     */
    protected class ComparableAlternative implements Comparable<ComparableAlternative> {
        private double value = 0;
        
        private Alternative alternative;
        
        public ComparableAlternative(Alternative alternative, IAggregator aggregator, TreeNode root) {
            this.alternative = alternative;
            this.value = aggregator.getAggregatedValue(root, alternative);
        }
        
        public double getValue() {
            return value;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         * 
         * We use the aggreated value for comparison (which alternative is the best).
         * In case of equal values we use alphabetical order (the alternative's name).
         */
        public int compareTo(ComparableAlternative o) {
            if(this.getValue() < o.getValue()) {
                return -1;
            } else if(this.getValue() > o.getValue()) {
                return 1;
            } else {
                return this.alternative.getName().compareTo(o.alternative.getName());
            }
        }
        
        public boolean equals(Object other) {
            if(other instanceof ComparableAlternative) {
                ComparableAlternative oa = (ComparableAlternative) other;
                return this.alternative.equals(oa.alternative);
            } else {
                return false;
            }
        }
        
        public int hashCode() {
            return 13 * alternative.hashCode();
        }
        
        public String toString() {
            return alternative.getName() + ": " + value;
        }
    }

    protected SortedSet<ComparableAlternative> normalOrder;
    
    protected List<Alternative> alternatives;
    
    private TreeNode root;
    
    private IAggregator aggregator;
    
    private boolean orderChanged = false;
    
    /**
     * Constructs a new ChangeOrderTest.
     * @param root The root node of the tree.
     * @param aggregator The aggregator used to evaluate the alternatives. This should be the same aggregator that is used for evaluating the alternatives.
     * @param alternatives The alternatives that should be evaluated during the test.
     */
    public OrderChangeTest(TreeNode root, IAggregator aggregator, List<Alternative> alternatives) {
        this.root = root;
        this.aggregator = aggregator;
        this.alternatives = alternatives;
        normalOrder = getOrder();
    }
    
    /**
     * Evaluate all alternatives using the aggregator given at creation time.
     * @return A set of alternatives sorted by their value (highest first).
     */
    protected SortedSet<ComparableAlternative> getOrder() {
        SortedSet<ComparableAlternative> result = new TreeSet<ComparableAlternative>();
        for(Alternative a : alternatives) {
            result.add(new ComparableAlternative(a, aggregator, root));
        }
        return result;
    }
    
    public void afterIteration(ResultNode node) {
        SortedSet<ComparableAlternative> order = getOrder();
        // unfortunately we cannot compare order and normalOrder using equals()
        Iterator<ComparableAlternative> normalOrderIterator = normalOrder.iterator();
        if(order.size() != normalOrder.size()) {
            // huh?! how did this happen? someone has modified the alternatives list
            return;
        }
        
        //we iterate over both sets (the "normal order" and current order).
        for(ComparableAlternative ca : order) {
            // this is actually an inner loop - notice the next().
            if(!ca.equals(normalOrderIterator.next())) {
                orderChanged = true;
            }
        }
        
    }

    public void afterNode(ResultNode node) {
       node.setSensitivityAnalysisResult(new OrderChangeResult(orderChanged));
    }

    public void beforeIteration(ResultNode node) {

    }

    public void beforeNode(ResultNode node) {
        orderChanged = false;
    }
    
    private class OrderChangeResult implements ISensitivityAnalysisResult {

        private boolean changed;

        public OrderChangeResult(boolean changed) {
            this.changed = changed;
        }
        
        public String toString() {
            if(changed) {
                return "!";
            } else {
                return "";
            }
        }

        public boolean isSensitive() {
            return changed;
        }

        public double getSensitivityCoefficient() {
            return changed ? 1 : 0;
        }

        public double getSensitivityThreashold() {
            return 0.5;
        }
    }


}
