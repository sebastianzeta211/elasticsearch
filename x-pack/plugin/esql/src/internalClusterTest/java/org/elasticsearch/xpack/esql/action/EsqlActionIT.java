/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.esql.action;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.compute.Experimental;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.hamcrest.ElasticsearchAssertions;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.elasticsearch.xpack.esql.plugin.EsqlPlugin;
import org.junit.Assert;
import org.junit.Before;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.elasticsearch.test.ESIntegTestCase.Scope.SUITE;

@Experimental
@ESIntegTestCase.ClusterScope(scope = SUITE, numDataNodes = 1, numClientNodes = 0, supportsDedicatedMasters = false)
@TestLogging(value = "org.elasticsearch.xpack.esql.session:DEBUG", reason = "to better understand planning")
public class EsqlActionIT extends ESIntegTestCase {

    @Before
    public void setupIndex() {
        ElasticsearchAssertions.assertAcked(
            client().admin()
                .indices()
                .prepareCreate("test")
                .setSettings(Settings.builder().put("index.number_of_shards", ESTestCase.randomIntBetween(1, 5)))
                .get()
        );
        for (int i = 0; i < 10; i++) {
            client().prepareBulk()
                .add(new IndexRequest("test").id("1" + i).source("data", 1, "count", 42))
                .add(new IndexRequest("test").id("2" + i).source("data", 2, "count", 44))
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .get();
        }
        ensureYellow("test");
    }

    public void testRow() {
        int value = randomIntBetween(0, Integer.MAX_VALUE);
        EsqlQueryResponse response = run("row" + value);
        assertEquals(List.of(List.of(value)), response.values());
    }

    public void testFromStats() {
        EsqlQueryResponse results = run("from test | stats avg(count)");
        logger.info(results);
        Assert.assertEquals(1, results.columns().size());
        Assert.assertEquals(1, results.values().size());
        assertEquals("avg(count)", results.columns().get(0).name());
        assertEquals("double", results.columns().get(0).type());
        assertEquals(1, results.values().get(0).size());
        assertEquals(43, (double) results.values().get(0).get(0), 1d);
    }

    public void testFrom() {
        EsqlQueryResponse results = run("from test");
        logger.info(results);
        Assert.assertEquals(20, results.values().size());
    }

    public void testFromSortLimit() {
        EsqlQueryResponse results = run("from test | sort count | limit 1");
        logger.info(results);
        Assert.assertEquals(1, results.values().size());
        assertEquals(42, (long) results.values().get(0).get(results.columns().indexOf(new ColumnInfo("count", "long"))));
    }

    public void testFromEvalSortLimit() {
        EsqlQueryResponse results = run("from test | eval x = count + 7 | sort x | limit 1");
        logger.info(results);
        Assert.assertEquals(1, results.values().size());
        assertEquals(49, (long) results.values().get(0).get(results.columns().indexOf(new ColumnInfo("x", "long"))));
    }

    public void testFromStatsEval() {
        EsqlQueryResponse results = run("from test | stats avg_count = avg(count) | eval x = avg_count + 7");
        logger.info(results);
        Assert.assertEquals(1, results.values().size());
        assertEquals(2, results.values().get(0).size());
        assertEquals(50, (double) results.values().get(0).get(results.columns().indexOf(new ColumnInfo("x", "double"))), 1d);
    }

    private EsqlQueryResponse run(String esqlCommands) {
        return new EsqlQueryRequestBuilder(client(), EsqlQueryAction.INSTANCE).query(esqlCommands).get();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singletonList(EsqlPlugin.class);
    }
}
