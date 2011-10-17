
ant.mkdir(dir:"${basedir}/grails-app/conf/security")
ant.copy(todir:"${basedir}/grails-app/conf/security") {
	fileset(dir:"${pluginBasedir}/grails-app/conf/security") {
		include(name:"*.*")
	}
}

ant.copy(file:"${pluginBasedir}/grails-app/conf/SamlSecurityConfig.groovy",
         todir:"${basedir}/grails-app/conf")	

println '''
*******************************************************
* You've installed the Spring Security Saml plugin.   *
*                                                     *
* Base configuration files and initial keystore have  *
* been create under the conf folder                   *
*******************************************************
'''