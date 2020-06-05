pipeline {
  agent none
  environment {
    SERVICE_NAME = 'scs'
    DOCKER_IMAGE = 'bremersee/scs'
    DEV_TAG = 'snapshot'
    PROD_TAG = 'latest'
    PUSH_SNAPSHOT = false
    PUSH_RELEASE = true
    SNAPSHOT_SITE = false
    RELEASE_SITE = true
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '8', artifactNumToKeepStr: '8'))
  }
  stages {
    stage('Test') {
      agent {
        label 'maven'
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      when {
        not {
          branch 'feature/*'
        }
      }
      steps {
        sh 'java -version'
        sh 'mvn -B --version'
        sh 'mvn -B clean test'
      }
      post {
        always {
          junit '**/surefire-reports/*.xml'
          jacoco(
              execPattern: '**/coverage-reports/*.exec'
          )
        }
      }
    }
    stage('Push snapshot') {
      agent {
        label 'maven'
      }
      when {
        allOf {
          branch 'develop'
          environment name: 'PUSH_SNAPSHOT', value: 'true'
        }
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh '''
          mvn -B -DskipTests -Ddockerfile.skip=false clean package dockerfile:push
          mvn -B -DskipTests -Ddockerfile.skip=false -Ddockerfile.tag=snapshot clean package dockerfile:push
          docker system prune -a -f
        '''
      }
    }
    stage('Push release') {
      agent {
        label 'maven'
      }
      when {
        allOf {
          branch 'master'
          environment name: 'PUSH_RELEASE', value: 'true'
        }
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh '''
          mvn -B -DskipTests -Ddockerfile.skip=false clean package dockerfile:push
          mvn -B -DskipTests -Ddockerfile.skip=false -Ddockerfile.tag=latest clean package dockerfile:push
          docker system prune -a -f
        '''
      }
    }
    stage('Deploy snapshot site') {
      agent {
        label 'maven'
      }
      environment {
        CODECOV_TOKEN = credentials('scs-codecov-token')
      }
      when {
        allOf {
          branch 'develop'
          environment name: 'SNAPSHOT_SITE', value: 'true'
        }
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh 'mvn -B clean site-deploy'
      }
      post {
        always {
          sh 'curl -s https://codecov.io/bash | bash -s - -t ${CODECOV_TOKEN}'
        }
      }
    }
    stage('Deploy release site') {
      agent {
        label 'maven'
      }
      environment {
        CODECOV_TOKEN = credentials('scs-codecov-token')
      }
      when {
        allOf {
          branch 'master'
          environment name: 'RELEASE_SITE', value: 'true'
        }
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh 'mvn -B -P gh-pages-site clean site site:stage scm-publish:publish-scm'
      }
      post {
        always {
          sh 'curl -s https://codecov.io/bash | bash -s - -t ${CODECOV_TOKEN}'
        }
      }
    }
    stage('Test feature') {
      agent {
        label 'maven'
      }
      when {
        branch 'feature/*'
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh 'java -version'
        sh 'mvn -B --version'
        sh 'mvn -B -P feature,allow-features clean test'
      }
      post {
        always {
          junit '**/surefire-reports/*.xml'
          jacoco(
              execPattern: '**/coverage-reports/*.exec'
          )
        }
      }
    }
  }
}