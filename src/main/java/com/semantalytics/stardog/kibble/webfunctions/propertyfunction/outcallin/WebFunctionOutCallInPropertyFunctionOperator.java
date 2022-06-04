package com.semantalytics.stardog.kibble.webfunctions.propertyfunction.outcallin;

import com.complexible.stardog.StardogException;
import com.complexible.stardog.plan.QueryTerm;
import com.complexible.stardog.plan.SortType;
import com.complexible.stardog.plan.eval.ExecutionContext;
import com.complexible.stardog.plan.eval.operator.*;
import com.complexible.stardog.plan.eval.operator.impl.AbstractOperator;
import com.complexible.stardog.plan.eval.operator.impl.Solutions;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.semantalytics.stardog.kibble.webfunctions.StardogWasmInstance;
import com.stardog.stark.Value;
import com.stardog.stark.query.BindingSet;
import com.stardog.stark.query.SelectQueryResult;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Executable operator for the repeat function
 *
 * @author Michael Grove
 */
public final class WebFunctionOutCallInPropertyFunctionOperator extends AbstractOperator implements PropertyFunctionOperator {

    private ValueOrError wasmIRI ;

    /**
     * The current solution
     */
    private Solution solution;

    /**
     * The child argument
     */
    private final Optional<Operator> mArg;

    /**
     * The original node
     */
    private final WebFunctionOutCallInPropertyFunctionPlanNode mNode;

    /**
     * An iterator over the child solutions of this operator
     */
    private Iterator<Solution> mInputs = null;

    private StardogWasmInstance instance;

    private SelectQueryResult selectQueryResult;

    public WebFunctionOutCallInPropertyFunctionOperator(final ExecutionContext theExecutionContext,
                                                        final WebFunctionOutCallInPropertyFunctionPlanNode theNode,
                                                        final Operator theOperator) {
        super(theExecutionContext, SortType.UNSORTED);

        mNode = Preconditions.checkNotNull(theNode);
        mArg = Optional.of(theOperator);
        wasmIRI = theNode.getWasm();

        try {
            //TODO should check if it's an error before assumign it's a value
            instance = StardogWasmInstance.from(wasmIRI.value(), getMappings());
        } catch (ExecutionException | MalformedURLException e) {
            throw new StardogException(e);
        }
    }

    @Override
    protected Solution computeNext() {
        if (mInputs == null) {
            // first call to compute results, perform some init
            // either use our child's solutions, or if we don't have a child, create a single solution to use
            if (mArg.filter(theOp -> !(theOp instanceof EmptyOperator)).isPresent()) {
                // these are the variables the child arg will bind
                Set<Integer> aVars = Sets.newHashSet(mArg.get().getVars());

                // and these are the ones that the pf will bind
                aVars.addAll(mNode.getResultVars().stream().map(QueryTerm::getName).collect(Collectors.toList()));

                // now we create a solution that contains room for bindings for these variables
                final Solution aSoln = mExecutionContext.getSolutionFactory()
                        .variables(aVars)
                        .newSolution();

                // and transform the child solutions to this one large enough to accomodate our vars
                mInputs = Iterators.transform(mArg.get(), theSoln -> {
                    Solutions.copy(aSoln, theSoln);
                    return aSoln;
                });
            } else if (mNode.getInput().stream().allMatch(QueryTerm::isVariable)) {
                // no arg or empty operator and the input is a variable, there's nothing to repeat
                return endOfData();
            } else {
                final Set<Integer> aVars = Sets.newHashSet();

                aVars.addAll(mNode.getResultVars().stream().map(QueryTerm::getName).collect(Collectors.toList()));

                // we only want to create solutions with the minimum number of variables
                mInputs = Iterators.singletonIterator(mExecutionContext.getSolutionFactory()
                        .variables(aVars)
                        .newSolution());
            }
        }

        while (mInputs.hasNext()) {
            if (solution == null) {
                solution = mInputs.next();
            }
            if (selectQueryResult == null) {
                try {
                    ValueOrError[] valueOrErrors = mNode.getObjects().stream().map(queryTerm -> {
                        if (queryTerm.isVariable()) {
                            return solution.getValue(queryTerm.getName(), getMappings());
                        } else {
                            return queryTerm.getValue();
                        }
                    }).toArray(ValueOrError[]::new);

                    if (Arrays.stream(valueOrErrors).noneMatch(ValueOrError::isError)) {
                        selectQueryResult = instance.evaluate(Arrays.stream(valueOrErrors).skip(1).map(ValueOrError::value).toArray(Value[]::new));
                    } else {
                        instance.close();
                        instance = null;
                        return endOfData();
                    }
                } catch (IOException e) {
                    instance.close();
                    instance = null;
                    return endOfData();
                }
            }
            if(selectQueryResult.hasNext()) {
                BindingSet bindingSet = selectQueryResult.next();
                if (mNode.getResultVars().size() <= bindingSet.size()) {
                    IntStream.range(0, mNode.getResultVars().size()).forEach(i ->
                            solution.setValue(mNode.getResultVars().get(i).getName(), bindingSet.get(String.format("value_%d", i)), getMappings()));
                } else {
                    instance.close();
                    instance = null;
                    return endOfData();
                }
                return solution;
            } else {
                selectQueryResult = null;
                solution = null;
            }
        }
        instance.close();
        instance = null;
        return endOfData();
    }
            /*

            private long getValue() {
                return mNode.getInput().isConstant()
                        ? mNode.getInput().getIndex()
                        : mValue.get(mNode.getInput().getName());
            }

             */

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performReset() {
        mArg.ifPresent(Operator::reset);
        instance.close();
        instance = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Integer> getVars() {
        return mNode.getSubjects().stream()
                .filter(QueryTerm::isVariable)
                .map(QueryTerm::getName)
                .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(final OperatorVisitor theOperatorVisitor) {
        theOperatorVisitor.visit(this);
    }
}
