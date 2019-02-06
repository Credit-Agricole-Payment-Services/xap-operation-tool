#!/usr/bin/env groovy

pipeline {
	agent { label 'docker' }

	environment {
		DOCKER_IMAGE_BUILD = 'maven:3.6.0-jdk-8-alpine'
	}

	options {
		// General Jenkins job properties
		buildDiscarder(logRotator(numToKeepStr: '1'))
		// "wrapper" steps that should wrap the entire build execution
		timestamps()
		timeout(time: 60, unit: 'MINUTES')
	}

	triggers {
		pollSCM('@hourly')
	}

	stages {
		stage('Build') {
			steps {
				withDockerContainer(image: DOCKER_IMAGE_BUILD, args: env.DOCKER_RUN_ARGS) {
					withMaven(
							// Specific local maven repo to prevent corruption with multiple concurrent builds
							mavenLocalRepo: '.repository',
							// Maven settings.xml file defined with the Jenkins Config File Provider Plugin
							// Maven settings and global settings can also be defined in Jenkins Global Tools Configuration
							mavenSettingsConfig: 'pipeline-maven-settings',
							options: [junitPublisher()]
					) {
						sh '$MVN_CMD -B --fail-at-end -U -Dsonar.skip clean install'
					}
				}
			}
		}
		stage('Quality Analysis') {
			when {
				anyOf { branch 'master'; branch 'develop'; branch 'dev'  }
			}
			steps {
				withDockerContainer(image: DOCKER_IMAGE_BUILD, args: env.DOCKER_RUN_ARGS) {
					withMaven(
							mavenLocalRepo: '.repository',
							mavenSettingsConfig: 'pipeline-maven-settings'
					) {
						sh '$MVN_CMD -B dependency:purge-local-repository -DmanualInclude=org.apache.maven.plugins:maven-deploy-plugin'
						sh '$MVN_CMD -B deploy sonar:sonar -Dmaven.test.skip'
					}
				}
			}
		}
		stage('Purge') {
			steps {
				withDockerContainer(image: DOCKER_IMAGE_BUILD, args: env.DOCKER_RUN_ARGS) {
					withMaven(
							mavenSettingsConfig: 'pipeline-maven-settings',
							mavenLocalRepo: '.repository',
							options: [
									junitPublisher(disabled: true, ignoreAttachments: true),
									artifactsPublisher(disabled: true),
									openTasksPublisher(disabled: true)
							]
					) {
						echo "Supprime les artefacts du cache local (pour éviter la pollution de l'espace de stockage)"
						sh '$MVN_CMD -B build-helper:remove-project-artifact'
					}
				}
			}
		}
	}
}