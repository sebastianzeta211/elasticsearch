/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.esql.plan.physical;

import org.elasticsearch.compute.Experimental;
import org.elasticsearch.xpack.ql.expression.Attribute;
import org.elasticsearch.xpack.ql.expression.Expression;
import org.elasticsearch.xpack.ql.expression.Expressions;
import org.elasticsearch.xpack.ql.expression.NamedExpression;
import org.elasticsearch.xpack.ql.tree.NodeInfo;
import org.elasticsearch.xpack.ql.tree.Source;

import java.util.List;
import java.util.Objects;

@Experimental
public class AggregateExec extends UnaryExec {

    private final List<? extends Expression> groupings;
    private final List<? extends NamedExpression> aggregates;

    private final Mode mode;

    public enum Mode {
        SINGLE,
        PARTIAL, // maps raw inputs to intermediate outputs
        FINAL, // maps intermediate inputs to final outputs
    }

    public AggregateExec(
        Source source,
        PhysicalPlan child,
        List<? extends Expression> groupings,
        List<? extends NamedExpression> aggregates
    ) {
        super(source, child);
        this.groupings = groupings;
        this.aggregates = aggregates;
        this.mode = Mode.SINGLE;
    }

    public AggregateExec(
        Source source,
        PhysicalPlan child,
        List<? extends Expression> groupings,
        List<? extends NamedExpression> aggregates,
        Mode mode
    ) {
        super(source, child);
        this.groupings = groupings;
        this.aggregates = aggregates;
        this.mode = mode;
    }

    @Override
    protected NodeInfo<AggregateExec> info() {
        return NodeInfo.create(this, AggregateExec::new, child(), groupings, aggregates, mode);
    }

    @Override
    public AggregateExec replaceChild(PhysicalPlan newChild) {
        return new AggregateExec(source(), newChild, groupings, aggregates, mode);
    }

    public List<? extends Expression> groupings() {
        return groupings;
    }

    public List<? extends NamedExpression> aggregates() {
        return aggregates;
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public List<Attribute> output() {
        return Expressions.asAttributes(aggregates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupings, aggregates, mode, child());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        AggregateExec other = (AggregateExec) obj;
        return Objects.equals(groupings, other.groupings)
            && Objects.equals(aggregates, other.aggregates)
            && Objects.equals(mode, other.mode)
            && Objects.equals(child(), other.child());
    }

    @Override
    public boolean singleNode() {
        if (mode != Mode.PARTIAL) {
            return true;
        }
        return child().singleNode();
    }
}
