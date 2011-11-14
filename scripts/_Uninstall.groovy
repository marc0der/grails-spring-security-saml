includeTargets << new File("$springSecuritySamlPluginDir/scripts/_SamlCommon.groovy")

printMessage '''
*******************************************************
* You've sucessfully uninstalled the                  *
* Spring Security Saml plugin.                        *
*                                                     *
* Saml configuration files remain in                  *
* grails-app/conf/security and configuration params   *
* in grails-app/conf/Config.groovy. Remove these      *
* manually if required.                               *
*******************************************************
'''