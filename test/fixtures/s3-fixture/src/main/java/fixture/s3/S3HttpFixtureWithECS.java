/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package fixture.s3;

import com.sun.net.httpserver.HttpHandler;

import org.elasticsearch.rest.RestStatus;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class S3HttpFixtureWithECS extends S3HttpFixtureWithEC2 {

    public S3HttpFixtureWithECS(final String[] args) throws Exception {
        super(args);
    }

    public S3HttpFixtureWithECS(boolean enabled) {
        this(enabled, "ecs_bucket", "ecs_base_path", "ecs_access_key", "ecs_session_token");
    }

    public S3HttpFixtureWithECS(boolean enabled, String... args) {
        super(enabled, args);
    }

    public S3HttpFixtureWithECS(InetSocketAddress inetSocketAddress, String[] strings) {
        super(inetSocketAddress, strings);
    }

    public S3HttpFixtureWithECS() {
        this(true);
    }

    @Override
    protected HttpHandler createHandler(final String[] args) {
        final String ecsAccessKey = Objects.requireNonNull(args[2]);
        final String ecsSessionToken = Objects.requireNonNull(args[3], "session token is missing");
        final HttpHandler delegate = super.createHandler(args);

        return exchange -> {
            // https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html
            if ("GET".equals(exchange.getRequestMethod()) && exchange.getRequestURI().getPath().equals("/ecs_credentials_endpoint")) {
                final byte[] response = buildCredentialResponse(ecsAccessKey, ecsSessionToken).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(RestStatus.OK.getStatus(), response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
                return;
            }
            delegate.handle(exchange);
        };
    }

    public static void main(final String[] args) throws Exception {
        if (args == null || args.length < 6) {
            throw new IllegalArgumentException(
                "S3HttpFixtureWithECS expects 6 arguments [address, port, bucket, base path, ecs access id, ecs session token]"
            );
        }
        final S3HttpFixtureWithECS fixture = new S3HttpFixtureWithECS(args);
        fixture.startWithWait();
    }
}
