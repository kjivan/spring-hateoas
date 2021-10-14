pipeline {
	agent none

	triggers {
		pollSCM 'H/3 * * * *'
	}

	options {
		disableConcurrentBuilds()
		buildDiscarder(logRotator(numToKeepStr: '14'))
	}

	stages {
		stage('Publish OpenJDK 17 + Graphviz + jq docker image') {
			when {
				changeset "ci/Dockerfile"
			}
			agent any

			steps {
				script {
					def image = docker.build("springci/spring-hateoas-openjdk17-with-graphviz-and-jq", "ci/")
					docker.withRegistry('', 'hub.docker.com-springbuildmaster') {
						image.push()
					}
				}
			}
		}

		stage("test: baseline (JDK 17)") {
			agent {
				docker {
					image 'openjdk:17'
					args '-v $HOME/.m2:/tmp/jenkins-home/.m2'
				}
			}
			options { timeout(time: 30, unit: 'MINUTES') }
			steps {
				sh 'PROFILE=none ci/test.sh'
			}
		}

		stage('Deploy') {
			agent {
				docker {
					image 'springci/spring-hateoas-openjdk17-with-graphviz-and-jq:latest'
					args '-v $HOME/.m2:/tmp/jenkins-home/.m2'
				}
			}
			options { timeout(time: 20, unit: 'MINUTES') }

			environment {
				ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
				SONATYPE = credentials('oss-login')
				KEYRING = credentials('spring-signing-secring.gpg')
				PASSPHRASE = credentials('spring-gpg-passphrase')
			}

			steps {
				script {
					PROJECT_VERSION = sh(
							script: "ci/version.sh",
							returnStdout: true
					).trim()

					RELEASE_TYPE = 'snapshot'

					if (PROJECT_VERSION.matches(/.*-RC[0-9]+$/) || PROJECT_VERSION.matches(/.*-M[0-9]+$/)) {
						RELEASE_TYPE = "milestone"
					} else if (PROJECT_VERSION.endsWith('SNAPSHOT')) {
						RELEASE_TYPE = 'snapshot'
					} else if (PROJECT_VERSION.matches(/.*\.[0-9]+$/)) {
						RELEASE_TYPE = 'release'
					}

					if (RELEASE_TYPE == 'release') {
						sh "PROFILE=ci,central ci/build-and-deploy-to-maven-central.sh ${PROJECT_VERSION}"

						slackSend(
                                color: (currentBuild.currentResult == 'SUCCESS') ? 'good' : 'danger',
                                channel: '#spring-hateoas',
                                message: "@here Spring HATEOAS ${PROJECT_VERSION} is staged on Sonatype awaiting closure and release.")
					} else {
						sh "PROFILE=ci,${RELEASE_TYPE} ci/build-and-deploy-to-artifactory.sh"
					}
				}
			}
		}
		stage('Release documentation') {
			when {
				anyOf {
					branch 'main'
					branch 'release'
				}
			}
			agent {
				docker {
					image 'springci/spring-hateoas-openjdk17-with-graphviz-and-jq:latest'
					args '-v $HOME/.m2:/tmp/jenkins-home/.m2'
				}
			}
			options { timeout(time: 20, unit: 'MINUTES') }

			environment {
				ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
			}

			steps {
				script {
					sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -Pci,distribute ' +
							'-Dartifactory.server=https://repo.spring.io ' +
							"-Dartifactory.username=${ARTIFACTORY_USR} " +
							"-Dartifactory.password=${ARTIFACTORY_PSW} " +
							"-Dartifactory.distribution-repository=temp-private-local " +
							'-Dmaven.test.skip=true deploy -B'
				}
			}
		}
	}

	post {
		changed {
			script {
				slackSend(
						color: (currentBuild.currentResult == 'SUCCESS') ? 'good' : 'danger',
						channel: '#spring-hateoas',
						message: "${currentBuild.fullDisplayName} - `${currentBuild.currentResult}`\n${env.BUILD_URL}")
				emailext(
						subject: "[${currentBuild.fullDisplayName}] ${currentBuild.currentResult}",
						mimeType: 'text/html',
						recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']],
						body: "<a href=\"${env.BUILD_URL}\">${currentBuild.fullDisplayName} is reported as ${currentBuild.currentResult}</a>")
			}
		}
	}
}
