package com.semantalytics.stardog.kibble.webfunctions.propertyfunction.incallout;

import com.complexible.stardog.plan.AbstractPropertyFunctionNodeBuilder;
import com.complexible.stardog.plan.PlanNodes;
import com.complexible.stardog.plan.QueryTerm;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.List;

public class WebFunctionInCallOutPropertyFunctionPlanNodeBuilder extends AbstractPropertyFunctionNodeBuilder<WebFunctionInCallOutPropertyFunctionPlanNode> {

    public WebFunctionInCallOutPropertyFunctionPlanNodeBuilder() {
        arg(PlanNodes.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate() {
        super.validate();

        Preconditions.checkState(mObjects.stream().allMatch(QueryTerm::isVariable));
        Preconditions.checkState(!mSubjects.isEmpty() && mSubjects.get(0).isConstant());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected WebFunctionInCallOutPropertyFunctionPlanNode createNode(final ImmutableSet<Integer> theSubjVars,
                                                                      final ImmutableSet<Integer> theObjVars,
                                                                      final ImmutableSet<Integer> theContextVars,
                                                                      final ImmutableSet<Integer> theAllVars) {

        return new WebFunctionInCallOutPropertyFunctionPlanNode(mArg,
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
        return mSubjects.stream().skip(1).anyMatch(QueryTerm::isVariable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<QueryTerm> getInputs() {
        return mSubjects.stream().filter(QueryTerm::isVariable).collect(ImmutableList.toImmutableList());
    }
}
