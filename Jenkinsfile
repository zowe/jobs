#!groovy

/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018, 2019
 */


node('ibm-jenkins-slave-nvm') {
  def lib = library("jenkins-library@classes").org.zowe.jenkins_shared_library

  def pipeline = lib.pipelines.gradle.GradlePipeline.new(this)

  pipeline.admins.add("jackjia")

  pipeline.setup(
    github: [
      email                      : 'zowe.robot@gmail.com',
      usernamePasswordCredential : 'zowe-robot-github',
    ],
    artifactory: [
      url                        : 'https://gizaartifactory.jfrog.io/gizaartifactory',
      usernamePasswordCredential : 'GizaArtifactory',
    ]
  )

  // we have a custom build command
  pipeline.build()

  pipeline.test(
    name          : 'Unit',
    operation     : {
        sh './gradlew coverage'
    },
    junit         : '**/test-results/test/*.xml',
    htmlReports   : [
      [dir: "build/reports/jacoco/jacocoFullReport/html", files: "index.html", name: "Report: Code Coverage"],
      [dir: "jobs-api-server/build/reports/tests/test", files: "index.html", name: "Report: Unit Test"],
    ],
  )

  // TODO: define integration test

  pipeline.sonarScan()

  // how we packaging jars/zips
  pipeline.packaging(
      name: 'explorer-jobs',
      operation: {
          sh './gradlew packageJobsApiServer'
      }
  )

  // define we need publish stage
  pipeline.publish(
    // NOTE: task publishArtifacts will publish to lib-release-local because we don't have SNAPSHOT in version
    artifacts: [
      'jobs-zowe-server-package/build/distributions/jobs-server-zowe-package.zip'
    ]
  )

  // define we need release stage
  pipeline.release()

  pipeline.end()
}
