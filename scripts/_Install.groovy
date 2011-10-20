// Copy base configuration files into security folder
ant.mkdir(dir:"${basedir}/grails-app/conf/security")
ant.copy(todir:"${basedir}/grails-app/conf/security") {
	fileset(dir:"${pluginBasedir}/grails-app/conf/security") {
		include(name:"*.*")
	}
}

includeTargets << new File("$springSecuritySamlPluginDir/scripts/_SamlConfig.groovy")

updateConfig()

private void updateConfig() {
	def configFile = new File("$basedir/grails-app", 'conf/Config.groovy')
	if (configFile.exists()) {
		configFile.withWriterAppend {
			it.writeLine '\n// Added by the Spring Security SAML plugin:'
			it.writeLine "${baseConfig}"
		}
	}
}

println '''
*******************************************************
* You've installed the Spring Security Saml plugin.   *
*                                                     *
* Example configuration and initial keystore have     *
* been create under the conf folder                   *
*******************************************************
'''