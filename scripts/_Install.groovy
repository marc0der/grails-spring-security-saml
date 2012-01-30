includeTargets << new File("$springSecuritySamlPluginDir/scripts/_SamlCommon.groovy")
includeTargets << grailsScript("_GrailsEvents")

def message = '''
*******************************************************
* You've installed the Spring Security Saml plugin.   *
*                                                     *
* To configure example saml configuration run script  *
* saml-quickstart                                     *
*******************************************************
'''
event("StatusFinal", [message])