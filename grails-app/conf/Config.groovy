// configuration for plugin testing - will not be included in the plugin zip

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'

    warn   'org.mortbay.log'
}

grails {
	plugins {
		springsecurity {
			userLookup {
				userDomainClassName = 'test.TestSamlUser'
				usernamePropertyName = 'username'
				enabledPropertyName = 'enabled'
				passwordPropertyName = 'password'
				authoritiesPropertyName = 'roles'
				authorityJoinClassName = 'test.TestUserRole'
			}

			requestMap {
				className = 'test.TestRequestmap'
				urlField = 'urlPattern'
				configAttributeField = 'rolePattern'
			}

			authority {
				className = 'test.TestRole'
				nameField = 'auth'
			}
		}
	}
}

grails.plugins.springsecurity.saml.active = true
grails.plugins.springsecurity.saml.afterLoginUrl = '/metadata'
grails.plugins.springsecurity.saml.afterLogoutUrl = '/metadata'
grails.plugins.springsecurity.saml.metadata.defaultIdp = 'ping'
grails.plugins.springsecurity.saml.userGroupAttribute = "memberOf"
grails.plugins.springsecurity.saml.userGroupToRoleMapping = ['GRG.APP.DigitalCatalogue':"ROLE_USER"]
grails.plugins.springsecurity.saml.metadata.url = '/saml/metadata'
grails.plugins.springsecurity.saml.metadata.providers = ['ping':'security/idp-local.xml']
grails.plugins.springsecurity.saml.keyManager.storeFile = 'classpath:security/keystore.jks'
grails.plugins.springsecurity.saml.keyManager.storePass = 'nalle123'
grails.plugins.springsecurity.saml.keyManager.passwords = ['ping':'ping123']
grails.plugins.springsecurity.saml.keyManager.defaultKey = 'ping'

grails.plugins.springsecurity.saml.metadata.sp.file = 'security/sp.xml'
grails.plugins.springsecurity.saml.metadata.sp.spMetadataDefaults = [
								'local':true, 'alias':'test','securityProfile':'metaiop','signingKey':'ping',
								'encryptionKey':'ping', 'tlsKey':'ping','requireArtifactResolveSigned':false,
								'requireLogoutRequestSigned':false, 'requireLogoutResponseSigned':false]
								
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"