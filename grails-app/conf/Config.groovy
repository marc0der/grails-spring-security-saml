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

security {
	saml {
		active = true
		userAttributeMappings = [password: "password"]
		userGroupAttribute = "memberOf"
		userGroupToRoleMapping = ['GRG.APP.DigitalCatalogue':"ROLE_USER"]
		entryPoint {
			//idpSelectionPath = '/saml/idpSelection.gsp'
		}
		metadata{
			url = '/saml/metadata'
			providers = [
				// TODO : Would be better if this was a classpath ref
				'ping':'security/idp-local.xml'
			]
			defaultIdp = 'ssoSSCircle'
		}
		keyManager {
			storeFile = "classpath:security/keystore.jks"
			storePass = "nalle123"
			passwords = ['ping':'ping123']
			defaultKey = 'ping'
		}
					
		afterLoginUrl = '/metadata'
		afterLogoutUrl = '/metadata'
	}
}

grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"