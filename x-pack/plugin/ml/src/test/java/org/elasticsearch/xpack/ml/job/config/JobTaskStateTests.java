/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ml.job.config;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.test.AbstractXContentSerializingTestCase;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xpack.core.ml.job.config.JobState;
import org.elasticsearch.xpack.core.ml.job.config.JobTaskState;

public class JobTaskStateTests extends AbstractXContentSerializingTestCase<JobTaskState> {

    @Override
    protected JobTaskState createTestInstance() {
        return new JobTaskState(randomFrom(JobState.values()), randomLong(), randomAlphaOfLength(10), randomInstant());
    }

    @Override
    protected JobTaskState mutateInstance(JobTaskState instance) {
        return null;// TODO implement https://github.com/elastic/elasticsearch/issues/25929
    }

    @Override
    protected Writeable.Reader<JobTaskState> instanceReader() {
        return JobTaskState::new;
    }

    @Override
    protected JobTaskState doParseInstance(XContentParser parser) {
        return JobTaskState.fromXContent(parser);
    }

    @Override
    protected boolean supportsUnknownFields() {
        return true;
    }
}
