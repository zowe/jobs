/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018, 2020
 */

package org.zowe.jobs.spring;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;

import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    private static final String TITLE = "JES Jobs API";
    private static final String DESCRIPTION = "REST API for the JES Jobs Service";
    private static final String VERSION = "2.0.0";
    private static final GenericApiResponseCustomizer GENERIC_API_RESPONSE_CUSTOMIZER = new GenericApiResponseCustomizer();

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title(TITLE).description(DESCRIPTION).version(VERSION));
    }

    private static class GenericApiResponseCustomizer implements OpenApiCustomiser {
        private static final ApiResponse response401 = new ApiResponse().description("Unauthorized");
        private static final ApiResponse response403 = new ApiResponse().description("Forbidden");
        private static final ApiResponse response404 = new ApiResponse().description("Not Found");

        @Override
        public void customise(OpenAPI openApi) {
            openApi.getPaths().forEach((key, pathEntry) -> pathEntry.readOperations().forEach(op -> {
                        op.getResponses().addApiResponse("401", response401);
                        op.getResponses().addApiResponse("403", response403);
                        op.getResponses().addApiResponse("404", response404);
                    }
            ));
        }
    }

    @Bean
    public GroupedOpenApi all() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/api/**")
                .addOpenApiCustomiser(openApi -> openApi.setInfo(openApi.getInfo().version(VERSION)))
                .addOpenApiCustomiser(GENERIC_API_RESPONSE_CUSTOMIZER)
                .build();
    }

    @Bean
    public GroupedOpenApi v1() {
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch("/api/v1/**")
                .addOpenApiCustomiser(openApi -> openApi.setInfo(openApi.getInfo().version("1.0.0")))
                .build();
    }

    @Bean
    public GroupedOpenApi v2() {
        return GroupedOpenApi.builder()
                .group("v2")
                .pathsToMatch("/api/v2/**")
                .addOpenApiCustomiser(openApi -> openApi.setInfo(openApi.getInfo().version(VERSION)))
                .build();
    }
}