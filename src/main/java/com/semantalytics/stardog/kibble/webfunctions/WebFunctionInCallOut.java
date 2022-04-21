package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.common.base.Pair;
import com.complexible.stardog.StardogException;
import com.complexible.stardog.index.statistics.Cardinality;
import com.complexible.stardog.plan.*;
import com.complexible.stardog.plan.eval.ExecutionContext;
import com.complexible.stardog.plan.eval.TranslateException;
import com.complexible.stardog.plan.eval.operator.*;
import com.complexible.stardog.plan.eval.operator.impl.AbstractOperator;
import com.complexible.stardog.plan.eval.operator.impl.Solutions;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.util.QueryTermRenderer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.stardog.stark.IRI;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.Values;
import com.stardog.stark.query.SelectQueryResult;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Instance;
import io.github.kawamuray.wasmtime.Val;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.stardog.stark.Values.iri;
import static java.util.stream.Collectors.toList;

public class WebFunctionInCallOut implements PropertyFunction {

        private static final WebFunctionVocabulary names = WebFunctionVocabulary.IN_call_OUT;

        /**
         * {@inheritDoc}
         */
        @Override
        public List<IRI> getURIs() {
            return names.getNames().stream().map(Values::iri).collect(toList());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public WebFunctionPlanNodeBuilder newBuilder() {
            return new WebFunctionPlanNodeBuilder();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Operator translate(final ExecutionContext theExecutionContext, final PropertyFunctionPlanNode thePropertyFunctionPlanNode, final Operator theOperator) throws
                TranslateException {

            if (thePropertyFunctionPlanNode instanceof WebFunctionPlanNode) {
                return new WebFunctionOperator(theExecutionContext, (WebFunctionPlanNode) thePropertyFunctionPlanNode, theOperator);
            }
            else {
                throw new TranslateException("Invalid node type, cannot translate");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void estimate(final PropertyFunctionPlanNode theNode, Costs.CostingContext theContext) throws PlanException {
            Preconditions.checkArgument(theNode instanceof WebFunctionPlanNode);

            //TODO should we add a method to wasm to provide an estimate???

            theNode.setCardinality(Cardinality.UNKNOWN);

            // assume a flat cost of 1 per iteration + the cost of our child
            theNode.setCost(theNode.getCardinality().value() + theNode.getArg().getCost());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String explain(final PropertyFunctionPlanNode theNode, final QueryTermRenderer theTermRenderer) {
            Preconditions.checkArgument(theNode instanceof WebFunctionPlanNode);
            Preconditions.checkNotNull(theTermRenderer);
            //TODO should this be added to the webassembly as well?

            final WebFunctionPlanNode aNode = (WebFunctionPlanNode) theNode;

            return String.format("Call WebFunction {}", aNode.getWasm());
        }

        /**
         * Representation of the property function as a `PlanNode`. This is used to represent the function within a query plan.
         *
         * @author Michael Grove
         */
        public static final class WebFunctionPlanNode extends AbstractPropertyFunctionPlanNode {

            private WebFunctionPlanNode(final PlanNode theArg,
                                        final List<QueryTerm> theSubjects,
                                        final List<QueryTerm> theObjects,
                                        final QueryTerm theContext,
                                        final QueryDataset.Scope theScope,
                                        final double theCost,
                                        final Cardinality theCardinality,
                                        final ImmutableSet<Integer> theSubjVars,
                                        final ImmutableSet<Integer> thePredVars,
                                        final ImmutableSet<Integer> theObjVars,
                                        final ImmutableSet<Integer> theContextVars,
                                        final ImmutableSet<Integer> theAssuredVars,
                                        final ImmutableSet<Integer> theAllVars) {
                super(theArg,
                        theSubjects,
                        theObjects,
                        theContext,
                        theScope,
                        theCost,
                        theCardinality,
                        theSubjVars,
                        thePredVars,
                        theObjVars,
                        theContextVars,
                        theAssuredVars,
                        theAllVars);
            }

            public ValueOrError getWasm() { return getSubjects().get(0).getValue(); }

            public List<QueryTerm> getInput() {
                return getSubjects().stream().collect(toList());
            }

            public List<QueryTerm> getResultVars() {
                return getObjects();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ImmutableList<QueryTerm> getInputs() {
                return getSubjects();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public IRI getURI() {
                return iri(names.getImmutableName());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public WebFunctionPlanNode copy() {
                return new WebFunctionPlanNode(getArg().copy(),
                                               getSubjects(),
                                               getObjects(),
                                               getContext(),
                                               getScope(),
                                               getCost(),
                                               getCardinality(),
                                               getSubjectVars(),
                                               getPredicateVars(),
                                               getObjectVars(),
                                               getContextVars(),
                                               getAssuredVars(),
                                               getAllVars());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected PropertyFunctionNodeBuilder createBuilder() {
                return new WebFunctionPlanNodeBuilder();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected boolean canEquals(final Object theObj) {
                return theObj instanceof WebFunctionPlanNode;
            }
        }

        /**
         * Basic builder for creating a {@link WebFunctionPlanNode}
         *
         * @author  Michael Grove
         */
        public static final class WebFunctionPlanNodeBuilder extends AbstractPropertyFunctionNodeBuilder<WebFunctionPlanNode> {

            public WebFunctionPlanNodeBuilder() {
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
            protected WebFunctionPlanNode createNode(final ImmutableSet<Integer> theSubjVars,
                                                     final ImmutableSet<Integer> theObjVars,
                                                     final ImmutableSet<Integer> theContextVars,
                                                     final ImmutableSet<Integer> theAllVars) {

                return new WebFunctionPlanNode(mArg,
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

        /**
         * Executable operator for the repeat function
         *
         * @author Michael Grove
         */
        public static final class WebFunctionOperator extends AbstractOperator implements PropertyFunctionOperator {

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
            private final WebFunctionPlanNode mNode;

            /**
             * An iterator over the child solutions of this operator
             */
            private Iterator<Solution> mInputs = null;

            private Instance instance;

            private SelectQueryResult selectQueryResult;

            public WebFunctionOperator(final ExecutionContext theExecutionContext,
                                       final WebFunctionPlanNode theNode,
                                       final Operator theOperator) {
                super(theExecutionContext, SortType.UNSORTED);

                mNode = Preconditions.checkNotNull(theNode);
                mArg = Optional.of(theOperator);
                wasmIRI = theNode.getWasm();

                try {
                    final URL wasmUrl = StardogWasm.getWasmUrl(mNode.getWasm().value());
                    //TODO should check if it's an error before assumign it's a value
                    if (instance != null) {
                        instance.close();
                    }
                    instance = StardogWasm.initWasm(wasmUrl, getMappings());
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
                        try {
                            final AtomicReference<Instance> instanceRef = new AtomicReference<>(instance);
                            ValueOrError[] valueOrErrors = mNode.getObjects().stream().map(queryTerm -> {
                                if(queryTerm.isVariable()) {
                                    return solution.getValue(queryTerm.getName(), getMappings());
                                } else {
                                    return queryTerm.getValue();
                                }
                            }).toArray(ValueOrError[]::new);

                            if(Arrays.stream(valueOrErrors).noneMatch(ValueOrError::isError)) {
                                final Pair<Integer, Integer> input = StardogWasm.writeToWasmMemory(instanceRef, "memory", Arrays.stream(valueOrErrors).skip(1).map(ValueOrError::value).toArray(Value[]::new));

                                try (final Func evaluateFunction = instance.getFunc(StardogWasm.store, StardogWasm.WASM_FUNCTION_EVALUATE).get()) {
                                    final Integer output_pointer = evaluateFunction.call(StardogWasm.store, Val.fromI32(input.first))[0].i32();
                                    StardogWasm.free(instanceRef, input);
                                    selectQueryResult = StardogWasm.readFromWasmMemorySelectQueryResult(instanceRef, "memory", output_pointer);
                                }
                            } else {
                                return endOfData();
                            }
                        } catch (IOException e) {
                            return endOfData();
                            //return ValueOrError.Error;
                        }

                    }
                    if (selectQueryResult.hasNext()) {
                        selectQueryResult.next().forEach(binding ->
                                solution.setValue(Integer.valueOf(binding.name().replaceAll("value_", "")), binding.value(), getMappings())
                        );
                        return solution;
                    } else {
                        solution = null;
                    }
                }
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
}
