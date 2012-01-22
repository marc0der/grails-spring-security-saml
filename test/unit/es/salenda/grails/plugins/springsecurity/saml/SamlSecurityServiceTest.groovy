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

	def grailsUser, grailsApplication

	@Override
	protected void setUp() {
		super.setUp()
		
		registerMetaClass DefaultGrailsApplication
		mockOutDefaultGrailsApplication()
		grailsApplication = new DefaultGrailsApplication()

		mockOutSpringSecurityUtilsConfig()

		grailsUser = new GrailsUser('username', 'password', true, true, true, true, [], 1)
		
		def authToken = new UsernamePasswordAuthenticationToken(grailsUser.username, null)
		authToken.setDetails(grailsUser)
		
		SecurityContextHolder.metaClass.static.getContext = {
			new SecurityContextImpl()
		}
		
		SecurityContextImpl.metaClass.getAuthentication = {
			authToken
		}
		
		def samlUser = new TestSamlUser(username: grailsUser.username, password: 'password')
		mockDomain TestSamlUser, [samlUser]
		
		SamlSecurityService.metaClass.isLoggedIn = { true }
	}

	void testGetCurrentUserNotPersisted() {
		def service = new SamlSecurityService()
		def fakeConfig = [ saml: [ autoCreate: [ active: false ] ] ]
		service.config = fakeConfig
		service.grailsApplication = grailsApplication
		
		def user = service.getCurrentUser()
		assert user instanceof GrailsUser
		assert user.username == grailsUser.username
	}
	
	void testGetCurrentUserPersisted() {
		def service = new SamlSecurityService()
		def fakeConfig = [
			userLookup: [ userDomainClassName: USER_CLASS_NAME ],
			saml: [ autoCreate: [
					active: true,
					key: 'username' ] ] ]
		service.config = fakeConfig
		service.grailsApplication = grailsApplication

		def user = service.getCurrentUser()
		assert user instanceof TestSamlUser
		assert user.username == grailsUser.username
	}
}
