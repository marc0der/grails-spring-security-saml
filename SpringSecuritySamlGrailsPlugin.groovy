import org.codehaus.groovy.grails.compiler.GrailsClassLoader;
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.plugins.springsecurity.SecurityFilterPosition

import org.springframework.core.io.ClassPathResource;
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
import org.springframework.security.saml.metadata.ExtendedMetadata
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate
import org.springframework.security.saml.metadata.MetadataDisplayFilter
import org.springframework.security.saml.metadata.MetadataGenerator
import org.springframework.security.saml.metadata.CachingMetadataManager
import org.springframework.security.saml.log.SAMLDefaultLogger
import org.springframework.security.saml.key.JKSKeyManager
import org.springframework.security.saml.util.VelocityFactory
import org.springframework.security.saml.context.SAMLContextProviderImpl

import es.salenda.grails.plugins.springsecurity.saml.SpringSamlUserDetailsService
import es.salenda.grails.plugins.springsecurity.saml.GrailsSAMLAuthenticationProvider
import es.salenda.grails.plugins.springsecurity.saml.SamlTagLib
import es.salenda.grails.plugins.springsecurity.saml.SamlSecurityService

import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider
import org.opensaml.xml.parse.BasicParserPool

import org.apache.commons.httpclient.HttpClient

class SpringSecuritySamlGrailsPlugin {
    // the plugin version
    def version = "1.0.0.M2"
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
    def developers = [ [ name: "Feroz Panwaskar", email: "feroz.panwaskar@gmail.com" ], [ name: "Marco Vermeulen", email: "vermeulen.mp@gmail.com" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPSPRINGSECURITYSAML" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "https://github.com/marcoVermeulen/grails-spring-security-saml" ]

	def providers = []

    def doWithSpring = {
		def conf = SpringSecurityUtils.securityConfig
		if (!conf || !conf.active) {
			return
		}
		
		// Allow users to override saml configuration from default
		def samlConfigFile = 'SamlSecurityConfig'
		def resFile = new ClassPathResource("${samlConfigFile}.class", SpringSecuritySamlGrailsPlugin.class.getClassLoader())

		if (resFile.exists()) {
			SpringSecurityUtils.loadSecondaryConfig samlConfigFile
		} else {
			SpringSecurityUtils.loadSecondaryConfig 'DefaultSamlSecurityConfig'
		}
		
		// have to get again after overlaying DefaultOpenIdSecurityConfig
		conf = SpringSecurityUtils.securityConfig

		if (!conf.saml.active) {
			return
		}
		
		//Doing a println here just because spring-security-core does
		println 'Configuring Spring Security SAML ...'

		//Due to Spring DSL limitations, need to import these beans as XML definitions
		def beansFile = "classpath:security/springSecuritySamlBeans.xml"
		log.debug "Importing beans from ${beansFile}..."
		delegate.importBeans beansFile
		
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
				idpSelectionPath = idpSelectionPath 					// '/index.gsp'
			}
			defaultProfileOptions = ref('webProfileOptions')
		}
		
		webProfileOptions(WebSSOProfileOptions) {
			includeScoping = false
		}
		
		metadataFilter(MetadataDisplayFilter) {
			filterSuffix = conf.saml.metadata.url 						// '/saml/metadata'
		}
		
		metadataGenerator(MetadataGenerator)
			
		// TODO: Update to handle any type of meta data providers for default to file based instead http provider.
		log.debug "Dynamically defining bean metadata providers... "
		conf.saml.metadata.providers.each {k,v ->
				def providerBeanName = "extendedMetadataDelegate"
				
				println "Registering metadata key: ${k} and value: $v"
				
				"${providerBeanName}"(ExtendedMetadataDelegate) { extMetaDataDelegateBean ->
						def resource = new ClassPathResource(v)
						filesystemMetadataProvider(FilesystemMetadataProvider) { bean ->
							bean.constructorArgs = [resource.getFile()]
							parserPool = ref('parserPool')
						}

						extMetaDataDelegateBean.constructorArgs = [ref('filesystemMetadataProvider'), new ExtendedMetadata()]
				}

				providers << ref(providerBeanName)
		}
		
		metadata(CachingMetadataManager) { bean ->
			// At this point, due to Spring DSL limitations, only one provider 
			// can be defined so just picking the first one
			bean.constructorArgs = [providers[0]]
			providers = providers
			// defaultIDP = conf.saml.metadata.providers[conf.saml.metadata.defaultIdp]
		}
		
		userDetailsService(SpringSamlUserDetailsService) {
			grailsApplication = ref('grailsApplication')
			sessionFactory = ref('sessionFactory')
		}
		
		samlAuthenticationProvider(GrailsSAMLAuthenticationProvider) {
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

    def doWithApplicationContext = { applicationContext ->
		/*
		def metadata = applicationContext.getBean('metadata')
		def providerBeans = []
		providers.each {
			providerBeans << applicationContext.getBean(it.beanName)
		}
		metadata.setProviders(providerBeans)
		*/
    }

    def onChange = { event ->
        // TODO reload
    }

    def onConfigChange = { event ->
        // TODO reload
    }
}
