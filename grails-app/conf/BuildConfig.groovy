
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.release.scm.enabled = false

grails.project.dependency.resolution = {
    inherits ('global') {
		excludes "xml-apis"
	}
	log 'warn'

	repositories {
		grailsPlugins()
		grailsHome()
		grailsCentral()

		mavenCentral()
	}

    dependencies {
		build ("org.apache.ivy:ivy:2.2.0") {
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
    }

    plugins {
        build(":tomcat:$grailsVersion",
              ":hibernate:$grailsVersion",
              ":release:1.0.1",) {
            export = false
        }
 	 	compile ":spring-security-core:1.2.1"
		test ":code-coverage:1.2.5"
    }
}

codenarc.reports = {
	CodeNarcReport('xml') {
		outputFile = 'target/test-reports/CodeNarcReport.xml'
		title = 'CodeNarc Report'
	}
}
