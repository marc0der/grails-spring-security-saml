package es.salenda.grails.plugins.springsecurity.saml

import static es.salenda.grails.plugins.springsecurity.saml.UnitTestUtils.*
import grails.test.mixin.*

import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser
import org.junit.Before
import org.junit.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl

import test.TestSamlUser

@TestFor(SamlSecurityService)
@Mock(TestSamlUser)
class SamlSecurityServiceTest {

	def grailsUser, authToken

	@Before
	public void init() {
		grailsUser = new GrailsUser('username', 'password', true, true, true, true, [], 1)

		authToken = new UsernamePasswordAuthenticationToken(grailsUser.username, null)
		authToken.setDetails(grailsUser)

		SamlSecurityService.metaClass.static.isLoggedIn = { -> true }
		SecurityContextHolder.metaClass.static.getContext = { -> new SecurityContextImpl() }
		SecurityContextImpl.metaClass.getAuthentication = { -> authToken }

		def samlUser = new TestSamlUser(username: grailsUser.username, password: 'password')
		assert samlUser.save()

	}

	@Test
	void "getCurrentUser should return user from sesion when autocreate active flag is false"() {
		def fakeConfig = [ saml: [ autoCreate: [ active: false ] ] ]
		
		service.config = fakeConfig
		service.grailsApplication = grailsApplication

		def user = service.getCurrentUser()
		assert user instanceof GrailsUser
		assert user.username == grailsUser.username
	}

	@Test
	void "getCurrentUser should return user from the database when autocreate active flag is true"() {
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
	
	@Test
	void "getCurrentUser should return null when the user is not logged in"() {
		SamlSecurityService.metaClass.static.isLoggedIn = { -> false }
		assert !service.getCurrentUser()
	}
	
	@Test
	void "getCurrentUser should return null when autocreate active and details from session is null"() {
		def fakeConfig = [saml: [ autoCreate: [active: true,] ] ]

		service.config = fakeConfig
		authToken.setDetails(null)
		
		assert !service.getCurrentUser()
	}
}
