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
package eu.scape_project.planning.model.kbrowser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.scape_project.planning.model.SampleAggregationMode;
import eu.scape_project.planning.model.TargetValueObject;
import eu.scape_project.planning.model.Values;
import eu.scape_project.planning.model.measurement.Measure;
import eu.scape_project.planning.model.scales.FloatRangeScale;
import eu.scape_project.planning.model.scales.FloatScale;
import eu.scape_project.planning.model.scales.IntRangeScale;
import eu.scape_project.planning.model.scales.IntegerScale;
import eu.scape_project.planning.model.scales.PositiveFloatScale;
import eu.scape_project.planning.model.scales.PositiveIntegerScale;
import eu.scape_project.planning.model.scales.Scale;
import eu.scape_project.planning.model.transform.NumericTransformer;
import eu.scape_project.planning.model.transform.OrdinalTransformer;
import eu.scape_project.planning.model.transform.Transformer;
import eu.scape_project.planning.model.values.INumericValue;
import eu.scape_project.planning.model.values.IOrdinalValue;
import eu.scape_project.planning.model.values.TargetValue;
import eu.scape_project.planning.model.values.Value;

@Entity
public class VPlanLeaf {
    private static final Logger log = LoggerFactory.getLogger(VPlanLeaf.class);

    @Id
    private int id;

    private int planId;

    /**
     * The weight of this leaf.
     */
    private double weight;

    /**
     * The aggregated weight up to the root, that means the impact of this leaf
     * on the overall result
     */
    private double totalWeight;

    @OneToOne
    private Scale scale;

    @OneToOne
    private Transformer transformer;

    @OneToOne
    private Measure measure;

    @Enumerated
    private SampleAggregationMode aggregationMode;

    // Do we need this, as this is read only
    // cascade = CascadeType.ALL, orphanRemoval = true
    @OneToMany(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    private Map<String, Values> valueMap = new HashMap<String, Values>();

    /**
     * Method responsible for assessing the potential output range of this
     * requirement leaf. Calculation rule: if (minPossibleTransformedValue == 0)
     * koFactor = 1; else koFactor = 0; potentialOutputRange = relativeWeight *
     * (maxPossibleTransformedValue - minPossibleTransformedValue) + koFactor;
     * 
     * @return potential output range. If the corresponding plan is not yet at a
     *         evaluation stage where actual output range can be calculated -1
     *         is returned ("ignore value").
     */
    public double getPotentialOutputRange() {
        // If the plan is not yet at a evaluation stage where potential output
        // range can be calculated - return 0.
        if (transformer == null) {
            return 0;
        }

        return totalWeight * (getPotentialMaximum() - getPotentialMinimum());
    }

    public double getPotentialMinimum() {
        // If the plan is not yet at a evaluation stage where potential output
        // range can be calculated - return 0.
        if (transformer == null) {
            return 0;
        }

        double outputLowerBound = 10;

        // Check OrdinalTransformer
        if (transformer instanceof OrdinalTransformer) {
            OrdinalTransformer ot = (OrdinalTransformer) transformer;
            Map<String, TargetValueObject> otMapping = ot.getMapping();

            for (TargetValueObject tv : otMapping.values()) {
                if (tv.getValue() < outputLowerBound) {
                    outputLowerBound = tv.getValue();
                }
            }
        }

        // Check NumericTransformer
        if (transformer instanceof NumericTransformer) {

            double scaleLowerBound = getScaleLowerBound();
            double scaleUpperBound = getScaleUpperBound();

            // get Transformer thresholds
            NumericTransformer nt = (NumericTransformer) transformer;

            // calculate output bounds
            // increasing thresholds
            if (nt.getThreshold1() <= nt.getThreshold5()) {
                // lower bound
                if (scaleLowerBound < nt.getThreshold1()) {
                    outputLowerBound = 0;
                } else if (scaleLowerBound < nt.getThreshold2()) {
                    outputLowerBound = 1;
                } else if (scaleLowerBound < nt.getThreshold3()) {
                    outputLowerBound = 2;
                } else if (scaleLowerBound < nt.getThreshold4()) {
                    outputLowerBound = 3;
                } else if (scaleLowerBound < nt.getThreshold5()) {
                    outputLowerBound = 4;
                } else {
                    outputLowerBound = 5;
                }
            }

            // decreasing thresholds
            if (nt.getThreshold1() > nt.getThreshold5()) {
                // lower bound
                if (scaleUpperBound > nt.getThreshold1()) {
                    outputLowerBound = 0;
                } else if (scaleUpperBound > nt.getThreshold2()) {
                    outputLowerBound = 1;
                } else if (scaleUpperBound > nt.getThreshold3()) {
                    outputLowerBound = 2;
                } else if (scaleUpperBound > nt.getThreshold4()) {
                    outputLowerBound = 3;
                } else if (scaleUpperBound > nt.getThreshold5()) {
                    outputLowerBound = 4;
                } else {
                    outputLowerBound = 5;
                }
            }
        }

        return outputLowerBound;
    }

    public double getPotentialMaximum() {
        // If the plan is not yet at a evaluation stage where potential output
        // range can be calcu)lated - return 0.
        if (transformer == null) {
            return 0;
        }

        double outputUpperBound = -10;

        // Check OrdinalTransformer
        if (transformer instanceof OrdinalTransformer) {
            OrdinalTransformer ot = (OrdinalTransformer) transformer;
            Map<String, TargetValueObject> otMapping = ot.getMapping();

            // set upper- and lower-bound
            for (TargetValueObject tv : otMapping.values()) {
                if (tv.getValue() > outputUpperBound) {
                    outputUpperBound = tv.getValue();
                }
            }
        }

        // Check NumericTransformer
        if (transformer instanceof NumericTransformer) {
            // I have to identify the scale bounds before I can calculate the
            // output bounds.
            double scaleLowerBound = getScaleLowerBound();
            double scaleUpperBound = getScaleUpperBound();

            // get Transformer thresholds
            NumericTransformer nt = (NumericTransformer) transformer;

            // calculate output bounds
            // increasing thresholds
            if (nt.getThreshold1() <= nt.getThreshold5()) {
                // upper bound
                if (scaleUpperBound < nt.getThreshold1()) {
                    outputUpperBound = 0;
                } else if (scaleUpperBound < nt.getThreshold2()) {
                    outputUpperBound = 1;
                } else if (scaleUpperBound < nt.getThreshold3()) {
                    outputUpperBound = 2;
                } else if (scaleUpperBound < nt.getThreshold4()) {
                    outputUpperBound = 3;
                } else if (scaleUpperBound < nt.getThreshold5()) {
                    outputUpperBound = 4;
                } else {
                    outputUpperBound = 5;
                }
            }

            // decreasing thresholds
            if (nt.getThreshold1() > nt.getThreshold5()) {
                // upper bound
                if (scaleLowerBound > nt.getThreshold1()) {
                    outputUpperBound = 0;
                } else if (scaleLowerBound > nt.getThreshold2()) {
                    outputUpperBound = 1;
                } else if (scaleLowerBound > nt.getThreshold3()) {
                    outputUpperBound = 2;
                } else if (scaleLowerBound > nt.getThreshold4()) {
                    outputUpperBound = 3;
                } else if (scaleLowerBound > nt.getThreshold5()) {
                    outputUpperBound = 4;
                } else {
                    outputUpperBound = 5;
                }
            }
        }

        return outputUpperBound;
    }

    private double getScaleLowerBound() {
        // I have to identify the scale bounds before I can calculate the
        // output bounds.
        double scaleLowerBound = -Double.MAX_VALUE;

        // At Positive Scales lowerBound is 0, upperBound has to be fetched
        if (scale instanceof PositiveIntegerScale) {
            scaleLowerBound = 0;
        }
        if (scale instanceof PositiveFloatScale) {
            scaleLowerBound = 0;
        }

        // At Range Scales lowerBound and upperBound have to be fetched
        if (scale instanceof IntRangeScale) {
            IntRangeScale s = (IntRangeScale) scale;
            scaleLowerBound = s.getLowerBound();
        }
        if (scale instanceof FloatRangeScale) {
            FloatRangeScale s = (FloatRangeScale) scale;
            scaleLowerBound = s.getLowerBound();
        }

        return scaleLowerBound;
    }

    private double getScaleUpperBound() {
        // I have to identify the scale bounds before I can calculate the
        // output bounds.
        double scaleUpperBound = Double.MAX_VALUE;

        // At Positive Scales lowerBound is 0, upperBound has to be fetched
        if (scale instanceof PositiveIntegerScale) {
            PositiveIntegerScale s = (PositiveIntegerScale) scale;
            scaleUpperBound = s.getUpperBound();
        }
        if (scale instanceof PositiveFloatScale) {
            PositiveFloatScale s = (PositiveFloatScale) scale;
            scaleUpperBound = s.getUpperBound();
        }

        // At Range Scales lowerBound and upperBound have to be fetched
        if (scale instanceof IntRangeScale) {
            IntRangeScale s = (IntRangeScale) scale;
            scaleUpperBound = s.getUpperBound();
        }
        if (scale instanceof FloatRangeScale) {
            FloatRangeScale s = (FloatRangeScale) scale;
            scaleUpperBound = s.getUpperBound();
        }

        return scaleUpperBound;
    }

    /**
     * Method responsible for assessing the actual output range of this
     * requirement leaf. Calculation rule: if (minActualTransformedValue == 0)
     * koFactor = 1; else koFactor = 0; actualOutputRange = relativeWeight *
     * (maxActualTransformedValue - minActualTransformedValue) + koFactor;
     * 
     * @return actual output range. If the corresponding plan is not yet at a
     *         evaluation stage where actual output range can be calculated -1
     *         is returned ("ignore value").
     */
    public double getActualOutputRange() {
        // If the plan is not yet at a evaluation stage where actual output
        // range can be calculated - return 0.
        if (transformer == null) {
            return 0;
        }

        List<Double> alternativeAggregatedValues = getAlternativeResults();

        // calculate upper/lower bound
        double outputLowerBound = 10;
        double outputUpperBound = -10;

        for (Double aVal : alternativeAggregatedValues) {
            if (aVal > outputUpperBound) {
                outputUpperBound = aVal;
            }
            if (aVal < outputLowerBound) {
                outputLowerBound = aVal;
            }
        }

        double actualOutputRange = totalWeight * (outputUpperBound - outputLowerBound);

        return actualOutputRange;
    }

    /**
     * Method responsible for calculating and returning the numeric result of
     * each leaf alternative.
     * 
     * @return Numeric result of each leaf alternative
     */
    public List<Double> getAlternativeResults() {
        List<Double> alternativeAggregatedValues = new ArrayList<Double>();
        Boolean skipAlternativeBecauseOfErrors = false;

        // iterate each alternative and calculate for each alternative its
        // aggregated value
        for (String alternative : valueMap.keySet()) {
            List<Value> alternativeValues = valueMap.get(alternative).getList();
            List<Double> alternativeTransformedValues = new ArrayList<Double>();

            // collect alternativeTransformedValues
            for (Value alternativeValue : alternativeValues) {
                TargetValue targetValue;

                // do ordinal transformation
                if (transformer instanceof OrdinalTransformer) {
                    OrdinalTransformer ordTrans = (OrdinalTransformer) transformer;

                    if (alternativeValue instanceof IOrdinalValue) {
                        try {
                            targetValue = ordTrans.transform((IOrdinalValue) alternativeValue);
                        } catch (NullPointerException e) {
                            log.warn("Measurement of leaf doesn't match with OrdinalTransformer! Ignoring it!");
                            log.warn("MeasuredValue-id: " + alternativeValue.getId() + "; Transformer-id: "
                                + ordTrans.getId());
                            // FIXME: this is a workaround for a strange bug
                            // described in changeset 4342
                            skipAlternativeBecauseOfErrors = true;
                            continue;
                        }
                        alternativeTransformedValues.add(targetValue.getValue());
                    } else {
                        log.warn("getActualOutputRange(): INumericValue value passed to OrdinalTransformer - ignore value");
                    }
                }

                // do numeric transformation
                if (transformer instanceof NumericTransformer) {
                    NumericTransformer numericTrans = (NumericTransformer) transformer;

                    if (alternativeValue instanceof INumericValue) {
                        targetValue = numericTrans.transform((INumericValue) alternativeValue);
                        alternativeTransformedValues.add(targetValue.getValue());
                    } else {
                        log.warn("getActualOutputRange(): IOrdinalValue value passed to NumericTransformer - ignore value");
                    }
                }
            }

            // aggregate the transformed values
            double count = 0;
            double sum = 0;
            double minValue = 5;
            Double alternativeAggregatedValue;

            for (Double alternativeTransformedValue : alternativeTransformedValues) {
                count++;
                sum = sum + alternativeTransformedValue;
                if (alternativeTransformedValue < minValue) {
                    minValue = alternativeTransformedValue;
                }
            }

            if (aggregationMode == SampleAggregationMode.AVERAGE) {
                alternativeAggregatedValue = sum / count;
            } else {
                alternativeAggregatedValue = minValue;
            }

            if (!skipAlternativeBecauseOfErrors) {
                alternativeAggregatedValues.add(alternativeAggregatedValue);
            }
            skipAlternativeBecauseOfErrors = false;
        }
        return alternativeAggregatedValues;
    }

    /**
     * Method responsible for calculating and returning the numeric result of
     * each leaf alternative.
     * 
     * @return Numeric result of each leaf alternative
     */
    public Map<String, Double> getAlternativeResultsAsMap() {
        Map<String, Double> alternativeAggregatedValues = new HashMap<String, Double>();

        // iterate each alternative and calculate for each alternative its
        // aggregated value
        for (String alternative : valueMap.keySet()) {
            List<Value> alternativeValues = valueMap.get(alternative).getList();
            List<Double> alternativeTransformedValues = new ArrayList<Double>();
            Boolean skipAlternativeBecauseOfErrors = false;

            // collect alternativeTransformedValues
            for (Value alternativeValue : alternativeValues) {
                TargetValue targetValue;

                // do ordinal transformation
                if (transformer instanceof OrdinalTransformer) {
                    OrdinalTransformer ordTrans = (OrdinalTransformer) transformer;

                    if (alternativeValue instanceof IOrdinalValue) {
                        try {
                            targetValue = ordTrans.transform((IOrdinalValue) alternativeValue);
                        } catch (NullPointerException e) {
                            log.warn("Measurement of leaf doesn't match with OrdinalTransformer! Ignoring it!");
                            log.warn("MeasuredValue-id: " + alternativeValue.getId() + "; Transformer-id: "
                                + ordTrans.getId());
                            // FIXME: this is a workaround for a strange bug
                            // described in changeset 4342
                            skipAlternativeBecauseOfErrors = true;
                            continue;
                        }
                        alternativeTransformedValues.add(targetValue.getValue());
                    } else {
                        log.warn("getActualOutputRange(): INumericValue value passed to OrdinalTransformer - ignore value");
                    }
                }

                // do numeric transformation
                if (transformer instanceof NumericTransformer) {
                    NumericTransformer numericTrans = (NumericTransformer) transformer;

                    if (alternativeValue instanceof INumericValue) {
                        targetValue = numericTrans.transform((INumericValue) alternativeValue);
                        alternativeTransformedValues.add(targetValue.getValue());
                    } else {
                        log.warn("getActualOutputRange(): IOrdinalValue value passed to NumericTransformer - ignore value");
                    }
                }
            }

            // aggregate the transformed values
            double count = 0;
            double sum = 0;
            double minValue = 5;
            Double alternativeAggregatedValue;

            for (Double alternativeTransformedValue : alternativeTransformedValues) {
                count++;
                sum = sum + alternativeTransformedValue;
                if (alternativeTransformedValue < minValue) {
                    minValue = alternativeTransformedValue;
                }
            }

            if (aggregationMode == SampleAggregationMode.AVERAGE) {
                alternativeAggregatedValue = sum / count;
            } else {
                alternativeAggregatedValue = minValue;
            }

            if (!skipAlternativeBecauseOfErrors) {
                alternativeAggregatedValues.put(alternative, alternativeAggregatedValue);
            }
            skipAlternativeBecauseOfErrors = false;
        }
        return alternativeAggregatedValues;
    }

    /**
     * Method responsible for assessing the relative output range of this
     * requirement leaf. Calculation rule: relativeOutputRange =
     * actualOutputRange / potentialOutputRange
     * 
     * @return relative output range. If the corresponding plan is not yet at a
     *         evaluation stage where relative output range can be calculated -1
     *         is returned ("ignore value").
     */
    public double getRelativeOutputRange() {
        double actualOutputRange = getActualOutputRange();
        double potentialOutputRange = getPotentialOutputRange();

        if ((actualOutputRange == -1) || (potentialOutputRange == -1)) {
            return -1;
        }

        if (potentialOutputRange == 0) {
            return 0;
        }

        return actualOutputRange / potentialOutputRange;
    }

    /**
     * Method responsible for assessing if the leaf has KnockOut potential.
     * 
     * @return true if the leaf has KO potential, otherwise false.
     */
    public Boolean hasKOPotential() {
        // check OrdinalTransformer KnockOut potential
        if (transformer instanceof OrdinalTransformer) {
            OrdinalTransformer ot = (OrdinalTransformer) transformer;
            Map<String, TargetValueObject> otMapping = ot.getMapping();

            // if any string maps to value 0 -> KnockOut potential
            for (TargetValueObject tv : otMapping.values()) {
                if (tv.getValue() == 0) {
                    return true;
                }
            }
        }
        // NumericTransformer has KnockOut potential if the relatedScale allows
        // a 0 transformed value.
        else if (transformer instanceof NumericTransformer) {
            // IntegerScale and FloatScale have no restrictions -> always have
            // KO Potential
            if ((scale instanceof IntegerScale) || (scale instanceof FloatScale)) {
                return true;
            }

            double scaleLowerBound = Double.MIN_VALUE;
            double scaleUpperBound = Double.MAX_VALUE;

            // At Positive Scales lowerBound is 0, upperBound has to be fetched
            if (scale instanceof PositiveIntegerScale) {
                PositiveIntegerScale s = (PositiveIntegerScale) scale;
                scaleLowerBound = 0;
                scaleUpperBound = s.getUpperBound();
            }
            if (scale instanceof PositiveFloatScale) {
                PositiveFloatScale s = (PositiveFloatScale) scale;
                scaleLowerBound = 0;
                scaleUpperBound = s.getUpperBound();
            }

            // At Range Scales lowerBound and upperBound have to be fetched
            if (scale instanceof IntRangeScale) {
                IntRangeScale s = (IntRangeScale) scale;
                scaleLowerBound = s.getLowerBound();
                scaleUpperBound = s.getUpperBound();
            }
            if (scale instanceof FloatRangeScale) {
                FloatRangeScale s = (FloatRangeScale) scale;
                scaleLowerBound = s.getLowerBound();
                scaleUpperBound = s.getUpperBound();
            }

            // get Transformer tresholds
            NumericTransformer nt = (NumericTransformer) transformer;
            double transformerT1 = nt.getThreshold1();
            double transformerT5 = nt.getThreshold5();

            // check for KO-Potential
            if ((transformerT1 < transformerT5) && (scaleLowerBound < transformerT1)) {
                return true;
            }
            if ((transformerT1 > transformerT5) && (scaleUpperBound > transformerT1)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Method responsible for assessing the number of KnockOut values (=0) this
     * leaf produces.
     * 
     * @return number of KO values produced by this leaf.
     */
    public int getActualKO() {
        if (transformer == null) {
            return 0;
        }

        List<Double> alternativeResult = getAlternativeResults();

        int koCount = 0;

        for (Double result : alternativeResult) {
            if (result == 0) {
                koCount++;
            }
        }

        return koCount;
    }

    /**
     * Method responsible for returning the measured values of this leaf.
     * 
     * @return Measured values of this leaf.
     */
    public List<Value> getMeasuredValues() {
        List<Value> result = new ArrayList<Value>();
        for (String alternative : valueMap.keySet()) {
            List<Value> alternativeValues = valueMap.get(alternative).getList();
            result.addAll(alternativeValues);
        }

        return result;
    }

    /**
     * Checks if this leaf is mapped to a measure.
     * 
     * @return true if the leaf is mapped, false otherwise
     */
    public boolean isMapped() {
        return (measure != null);
    }

    // ---------- getter/setter ----------
    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }

    public void setTotalWeight(double totalWeight) {
        this.totalWeight = totalWeight;
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public void setScale(Scale scale) {
        this.scale = scale;
    }

    public Scale getScale() {
        return scale;
    }

    public void setTransformer(Transformer transformer) {
        this.transformer = transformer;
    }

    public Transformer getTransformer() {
        return transformer;
    }

    public void setMeasure(Measure measure) {
        this.measure = measure;
    }

    public Measure getMeasure() {
        return measure;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public int getPlanId() {
        return planId;
    }

    public void setValueMap(Map<String, Values> valueMap) {
        this.valueMap = valueMap;
    }

    public Map<String, Values> getValueMap() {
        return valueMap;
    }

    public void setAggregationMode(SampleAggregationMode aggregationMode) {
        this.aggregationMode = aggregationMode;
    }

    public SampleAggregationMode getAggregationMode() {
        return aggregationMode;
    }
}
