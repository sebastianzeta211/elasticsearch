/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.search.aggregations;

import org.apache.lucene.util.SetOnce;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.collect.Iterators;
import org.elasticsearch.common.util.Maps;
import org.elasticsearch.xcontent.ToXContentFragment;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.elasticsearch.common.xcontent.XContentParserUtils.parseTypedKeysObject;

/**
 * Represents a set of {@link Aggregation}s
 */
public class Aggregations implements Iterable<Aggregation>, ToXContentFragment {

    public static final String AGGREGATIONS_FIELD = "aggregations";

    protected final List<? extends Aggregation> aggregations;
    private Map<String, Aggregation> aggregationsAsMap;

    public Aggregations(List<? extends Aggregation> aggregations) {
        this.aggregations = aggregations;
        if (aggregations.isEmpty()) {
            aggregationsAsMap = emptyMap();
        }
    }

    /**
     * Iterates over the {@link Aggregation}s.
     */
    @Override
    public final Iterator<Aggregation> iterator() {
        return Iterators.map(aggregations.iterator(), p -> (Aggregation) p);
    }

    /**
     * The list of {@link Aggregation}s.
     */
    public final List<Aggregation> asList() {
        return Collections.unmodifiableList(aggregations);
    }

    /**
     * Returns the {@link Aggregation}s keyed by aggregation name.
     */
    public final Map<String, Aggregation> asMap() {
        return getAsMap();
    }

    /**
     * Returns the {@link Aggregation}s keyed by aggregation name.
     */
    public final Map<String, Aggregation> getAsMap() {
        if (aggregationsAsMap == null) {
            Map<String, Aggregation> newAggregationsAsMap = Maps.newMapWithExpectedSize(aggregations.size());
            for (Aggregation aggregation : aggregations) {
                newAggregationsAsMap.put(aggregation.getName(), aggregation);
            }
            this.aggregationsAsMap = unmodifiableMap(newAggregationsAsMap);
        }
        return aggregationsAsMap;
    }

    /**
     * Returns the aggregation that is associated with the specified name.
     */
    @SuppressWarnings("unchecked")
    public final <A extends Aggregation> A get(String name) {
        return (A) asMap().get(name);
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return aggregations.equals(((Aggregations) obj).aggregations);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), aggregations);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        if (aggregations.isEmpty()) {
            return builder;
        }
        builder.startObject(AGGREGATIONS_FIELD);
        toXContentInternal(builder, params);
        return builder.endObject();
    }

    /**
     * Directly write all the aggregations without their bounding object. Used by sub-aggregations (non top level aggs)
     */
    public XContentBuilder toXContentInternal(XContentBuilder builder, Params params) throws IOException {
        for (Aggregation aggregation : aggregations) {
            aggregation.toXContent(builder, params);
        }
        return builder;
    }

    public static Aggregations fromXContent(XContentParser parser) throws IOException {
        final List<Aggregation> aggregations = new ArrayList<>();
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.START_OBJECT) {
                SetOnce<Aggregation> typedAgg = new SetOnce<>();
                String currentField = parser.currentName();
                parseTypedKeysObject(parser, Aggregation.TYPED_KEYS_DELIMITER, Aggregation.class, typedAgg::set);
                if (typedAgg.get() != null) {
                    aggregations.add(typedAgg.get());
                } else {
                    throw new ParsingException(
                        parser.getTokenLocation(),
                        String.format(Locale.ROOT, "Could not parse aggregation keyed as [%s]", currentField)
                    );
                }
            }
        }
        return new Aggregations(aggregations);
    }
}
