/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018
 */

package org.zowe.jobs;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;

import java.lang.management.ManagementFactory;

@SpringBootApplication

// @EnableApiDiscovery
@ComponentScan({ "org.zowe" })
@Slf4j
public class JesJobsApplication implements ApplicationListener<ApplicationReadyEvent> {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(JesJobsApplication.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        log.info("ZWEE0000I {} started in {} seconds", JesJobsApplication.class.getSimpleName(), uptime / 1000.0);
    }
}
