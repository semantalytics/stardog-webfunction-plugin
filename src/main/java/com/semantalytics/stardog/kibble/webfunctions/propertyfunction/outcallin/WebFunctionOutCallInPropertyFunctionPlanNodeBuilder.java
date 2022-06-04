package com.semantalytics.stardog.kibble.webfunctions.propertyfunction.outcallin;

import com.complexible.stardog.plan.AbstractPropertyFunctionNodeBuilder;
import com.complexible.stardog.plan.PlanNodes;
import com.complexible.stardog.plan.QueryTerm;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * Representation of the property function as a `PlanNode`. This is used to represent the function within a query plan.
 *
 * @author Michael Grove
 */


/**
 * Basic builder for creating a {@link WebFunctionOutCallInPropertyFunctionPlanNode}
 *
 * @author  Michael Grove
 */
public final class WebFunctionOutCallInPropertyFunctionPlanNodeBuilder extends AbstractPropertyFunctionNodeBuilder<WebFunctionOutCallInPropertyFunctionPlanNode> {

    public WebFunctionOutCallInPropertyFunctionPlanNodeBuilder() {
        arg(PlanNodes.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate() {
        super.validate();

        Preconditions.checkState(mSubjects.stream().allMatch(QueryTerm::isVariable));
        Preconditions.checkState(!mObjects.isEmpty() && mObjects.get(0).isConstant());
        //TODO possibly relax constraint on constant with warning that not using a constant might kill performance
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected WebFunctionOutCallInPropertyFunctionPlanNode createNode(final ImmutableSet<Integer> theSubjVars,
                                                                      final ImmutableSet<Integer> theObjVars,
                                                                      final ImmutableSet<Integer> theContextVars,
                                                                      final ImmutableSet<Integer> theAllVars) {

        return new WebFunctionOutCallInPropertyFunctionPlanNode(mArg,
                mSubjects,
                mObjects,
                mContext,
                mScope,
                mCost,
                mCardinality,
                theSubjVars,
                ImmutableSet.of(),
                theObjVars,
                theContextVars,
                Sets.union(theSubjVars, theObjVars).immutableCopy(),
                theAllVars);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasInputs() {
        return mObjects.stream().skip(1).anyMatch(QueryTerm::isVariable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<QueryTerm> getInputs() {
        return mObjects.stream().filter(QueryTerm::isVariable).collect(toImmutableList());
    }
}


