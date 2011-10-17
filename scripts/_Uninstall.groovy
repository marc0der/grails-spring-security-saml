ant.delete(dir:"${basedir}/grails-app/conf/security")
ant.delete(file:"${basedir}/grails-app/conf/SamlSecurityConfig.groovy")

println '''
*******************************************************
* You've sucessfully uninstalled the                  *
* Spring Security Saml plugin.                        *
*                                                     *
* Saml configuration files have been removed from     *
* your project.                                       *
*******************************************************
'''