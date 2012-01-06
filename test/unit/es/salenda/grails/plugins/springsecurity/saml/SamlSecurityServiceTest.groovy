package es.salenda.grails.plugins.springsecurity.saml

import static es.salenda.grails.plugins.springsecurity.saml.UnitTestUtils.*
import grails.test.GrailsUnitTestCase

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken


import test.TestSamlUser

class SamlSecurityServiceTest extends GrailsUnitTestCase {

	def service, config, grailsUser, authToken, samlUser

	@Override
	protected void setUp() {
		super.setUp()
		service = new SamlSecurityService()
		
		registerMetaClass DefaultGrailsApplication
		mockOutDefaultGrailsApplication()
		service.grailsApplication = new DefaultGrailsApplication()

		grailsUser = new GrailsUser('username', 'password', true, true, true, true, [], 1)
		
		authToken = new UsernamePasswordAuthenticationToken(grailsUser.username, null)
		authToken.setDetails(grailsUser)
		
		SecurityContextHolder.metaClass.static.getContext = {
			new SecurityContextImpl()
		}
		
		SecurityContextImpl.metaClass.getAuthentication = {
			authToken
		}
		
		samlUser = new TestSamlUser(username: grailsUser.username, password: 'password')
		mockDomain TestSamlUser, [samlUser]
		
		config = setTestConfig()
	}

	void testGetCurrentUserNotPersisted() {
		
		SamlSecurityService.metaClass.isLoggedIn = { true }
		
		def user = service.getCurrentUser()
		assert user instanceof GrailsUser
		assert user.username == grailsUser.username
	}
	
	void testGetCurrentUserPersisted() {
		def additionalConfig = [saml:[
			autoCreate: [active: true, key: 'username'],
		]]
		config.putAll additionalConfig
		
		def user = service.getCurrentUser()
		assert user instanceof TestSamlUser
		assert user.username == grailsUser.username
	}
}
