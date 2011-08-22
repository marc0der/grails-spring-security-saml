import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.plugins.springsecurity.SecurityFilterPosition

import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.saml.SAMLBootstrap
import org.springframework.security.saml.SAMLEntryPoint
import org.springframework.security.saml.SAMLAuthenticationProvider
import org.springframework.security.saml.SAMLProcessingFilter
import org.springframework.security.saml.SAMLLogoutFilter
import org.springframework.security.saml.SAMLLogoutProcessingFilter
import org.springframework.security.saml.websso.WebSSOProfileOptions
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl
import org.springframework.security.saml.websso.WebSSOProfileImpl
import org.springframework.security.saml.websso.WebSSOProfileECPImpl
import org.springframework.security.saml.websso.SingleLogoutProfileImpl
import org.springframework.security.saml.websso.ArtifactResolutionProfileImpl
import org.springframework.security.saml.processor.HTTPPostBinding
import org.springframework.security.saml.processor.HTTPRedirectDeflateBinding
import org.springframework.security.saml.processor.HTTPArtifactBinding
import org.springframework.security.saml.processor.HTTPSOAP11Binding
import org.springframework.security.saml.processor.HTTPPAOS11Binding
import org.springframework.security.saml.processor.SAMLProcessorImpl
import org.springframework.security.saml.metadata.MetadataDisplayFilter
import org.springframework.security.saml.metadata.MetadataGenerator
import org.springframework.security.saml.metadata.CachingMetadataManager
import org.springframework.security.saml.log.SAMLDefaultLogger
import org.springframework.security.saml.key.JKSKeyManager
import org.springframework.security.saml.util.VelocityFactory
import org.springframework.security.saml.context.SAMLContextProviderImpl

import es.salenda.grails.plugins.springsecurity.saml.SamlUserDetails
import es.salenda.grails.plugins.springsecurity.saml.SamlTagLib
import es.salenda.grails.plugins.springsecurity.saml.SamlSecurityService

import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider
import org.opensaml.xml.parse.BasicParserPool

import org.apache.commons.httpclient.HttpClient

class SpringSecuritySamlGrailsPlugin {
    // the plugin version
    def version = "1.0.0.M1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3 > *"
    // the other plugins this plugin depends on
    def dependsOn = [springSecurityCore: '1.1 > *']
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "SAML 2.x support for the Spring Security Plugin" // Headline display name of the plugin
    def author = "Alvaro Sanchez-Mariscal"
    def authorEmail = "alvaro.sanchez@salenda.es"
    def description = '''\
SAML 2.x support for the Spring Security Plugin
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/spring-security-saml"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "Salenda", url: "http://www.salenda.es" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.grails-plugins.codehaus.org/browse/grails-plugins/" ]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }
	
	def providers = []

    def doWithSpring = {
		def conf = SpringSecurityUtils.securityConfig
		if (!conf || !conf.active) {
			return
		}

		SpringSecurityUtils.loadSecondaryConfig 'DefaultSamlSecurityConfig'
		// have to get again after overlaying DefaultOpenIdSecurityConfig
		conf = SpringSecurityUtils.securityConfig

		if (!conf.saml.active) {
			return
		}
		println 'Configuring Spring Security SAML ...'

		delegate.importBeans "classpath:security/springSecuritySamlBeans.xml"
		
		xmlns context:"http://www.springframework.org/schema/context"
		context.'annotation-config'()
		context.'component-scan'('base-package': "org.springframework.security.saml")
		
		SpringSecurityUtils.registerProvider 'samlAuthenticationProvider'
		SpringSecurityUtils.registerLogoutHandler 'successLogoutHandler'
		SpringSecurityUtils.registerLogoutHandler 'logoutHandler'
		SpringSecurityUtils.registerFilter 'samlEntryPoint', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 1
		SpringSecurityUtils.registerFilter 'metadataFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 2
		SpringSecurityUtils.registerFilter 'samlProcessingFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 3
		SpringSecurityUtils.registerFilter 'samlLogoutFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 4
		SpringSecurityUtils.registerFilter 'samlLogoutProcessingFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 5
		
		successRedirectHandler(SavedRequestAwareAuthenticationSuccessHandler) {
			defaultTargetUrl = conf.saml.afterLoginUrl
		}
		
		successLogoutHandler(SimpleUrlLogoutSuccessHandler) {
			defaultTargetUrl = conf.saml.afterLogoutUrl
		}
		
		samlLogger(SAMLDefaultLogger)
		
		keyManager(JKSKeyManager, 
			conf.saml.keyManager.storeFile, conf.saml.keyManager.storePass, conf.saml.keyManager.passwords, conf.saml.keyManager.defaultKey) 
		
		def idpSelectionPath = conf.saml.entryPoint.idpSelectionPath
		samlEntryPoint(SAMLEntryPoint) {
			filterSuffix = conf.auth.loginFormUrl 						// '/saml/login'
			if (idpSelectionPath) {
				idpSelectionPath = idpSelectionPath 	// '/index.gsp'
			}
			defaultProfileOptions = ref('webProfileOptions')
		}
		
		webProfileOptions(WebSSOProfileOptions) {
			includeScoping = false
		}
		
		metadataFilter(MetadataDisplayFilter) {
			filterSuffix = conf.saml.metadata.url // '/saml/metadata'
		}
		
		metadataGenerator(MetadataGenerator)
			
		conf.saml.metadata.providers.each {k,v ->
			def providerBeanName = "${k}HttpMetadataProvider"
			"${providerBeanName}"(HTTPMetadataProvider, v, 5000) {
				parserPool = ref('parserPool')
			}
			providers << ref(providerBeanName)
		}
		
		metadata(CachingMetadataManager) { bean ->
			bean.constructorArgs = [providers[0]]
			providers = providers
			defaultIDP = conf.saml.metadata.providers[conf.saml.metadata.defaultIdp]
		}
		
		userDetailsService(SamlUserDetails) {
			grailsApplication = ref('grailsApplication')
			sessionFactory = ref('sessionFactory')
		}
		
		samlAuthenticationProvider(SAMLAuthenticationProvider) {
			userDetails = ref('userDetailsService')
		}
		
		contextProvider(SAMLContextProviderImpl)
		
		samlProcessingFilter(SAMLProcessingFilter) {
			authenticationManager = ref('authenticationManager')
			authenticationSuccessHandler = ref('successRedirectHandler')
		}
		
		logoutHandler(SecurityContextLogoutHandler) {
			invalidateHttpSession = false
		}
		
		samlLogoutFilter(SAMLLogoutFilter, 
			ref('successLogoutHandler'), ref('logoutHandler'), ref('logoutHandler'))
		
		samlLogoutProcessingFilter(SAMLLogoutProcessingFilter, 
			ref('successLogoutHandler'), ref('logoutHandler'))
		
		webSSOprofileConsumer(WebSSOProfileConsumerImpl)
		
		webSSOprofile(WebSSOProfileImpl)
		
		ecpprofile(WebSSOProfileECPImpl)
		
		logoutprofile(SingleLogoutProfileImpl)
		
		postBinding(HTTPPostBinding, ref('parserPool'), ref('velocityEngine'))
		
		redirectBinding(HTTPRedirectDeflateBinding, ref('parserPool'))
		
		artifactBinding(HTTPArtifactBinding,
			ref('parserPool'), 
			ref('velocityEngine'),
			ref('artifactResolutionProfile')
		)
		
		artifactResolutionProfile(ArtifactResolutionProfileImpl, ref('httpClient')) {
			processor = ref('soapProcessor')
		}
		
		httpClient(HttpClient)
		
		soapProcessor(SAMLProcessorImpl, ref('soapBinding'))
		
		soapBinding(HTTPSOAP11Binding, ref('parserPool'))
		
		paosBinding(HTTPPAOS11Binding, ref('parserPool'))
		
		bootStrap(SAMLBootstrap)
		
		velocityEngine(VelocityFactory) { bean ->
			bean.factoryMethod = "getEngine"
		}
		
		parserPool(BasicParserPool)
		
		securityTagLib(SamlTagLib) {
			springSecurityService = ref('springSecurityService')
			webExpressionHandler = ref('webExpressionHandler')
			webInvocationPrivilegeEvaluator = ref('webInvocationPrivilegeEvaluator')
		}
		
		springSecurityService(SamlSecurityService) {
			authenticationTrustResolver = ref('authenticationTrustResolver')
			grailsApplication = ref('grailsApplication')
			passwordEncoder = ref('passwordEncoder')
			objectDefinitionSource = ref('objectDefinitionSource')
			userDetailsService = ref('userDetailsService')
			userCache = ref('userCache')
		}
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
		def metadata = applicationContext.getBean('metadata')
		def providerBeans = []
		providers.each {
			providerBeans << applicationContext.getBean(it.beanName)
		}
		metadata.setProviders(providerBeans)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
