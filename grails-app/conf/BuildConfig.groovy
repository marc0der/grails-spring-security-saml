grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.release.scm.enabled = false

grails.project.dependency.resolution = {
    inherits 'global'
	log 'warn'

	repositories {
		grailsPlugins()
		grailsHome()
		grailsCentral()

		mavenCentral()
	}

    dependencies {
		compile ('org.springframework.security.extensions:spring-security-saml2-core:1.0.0-RC1-SNAPSHOT') {
			transitive = false
		}
		compile ('org.opensaml:opensaml:2.4.1') {
			transitive = false
		}
		compile ('org.opensaml:xmltooling:1.3.1') {
			transitive = false
		}
		compile ('org.apache.santuario:xmlsec:1.4.4') {
			transitive = false
		}	
		compile ('org.bouncycastle:bcprov-jdk15:1.45') {
			transitive = false
		}
		compile ('org.opensaml:openws:1.4.1') {
			transitive = false
		}
		compile('joda-time:joda-time:1.6.2') {
			transitive = false
		}
		compile('commons-httpclient:commons-httpclient:3.1') {
			transitive = false
		}
		compile('org.apache.velocity:velocity:1.7') {
			transitive = false
		}
		compile ('ca.juliusdavies:not-yet-commons-ssl:0.3.9') {
			transitive = false 
		}
		/*compile('xerces:xercesImpl:2.8.1') {
			transitive = false
		}*/
    }

    plugins {
        build(":tomcat:$grailsVersion",
              ":release:1.0.0.RC3") {
            export = false
        }
    }
}
