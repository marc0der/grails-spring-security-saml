package es.salenda.grails.plugins.springsecurity.saml

import static es.salenda.grails.plugins.springsecurity.saml.UnitTestUtils.*

import grails.test.GrailsUnitTestCase
import groovy.util.ConfigObject

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser
import org.codehaus.groovy.grails.plugins.springsecurity.GormUserDetailsService
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.tools.groovydoc.MockOutputTool;
import org.opensaml.saml2.core.impl.AssertionImpl
import org.opensaml.saml2.core.impl.NameIDImpl
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.saml.*

import test.TestRole
import test.TestSamlUser
import test.TestUserRole

class SpringSamlUserDetailsServiceTest extends GrailsUnitTestCase {
	def credential, nameID, assertion, mockGrailsAplication

	String username = "jackSparrow"
	Map detailsServiceSettings = [:]
	DefaultGrailsApplication grailsApplication

	@Override
	protected void setUp() {
		super.setUp()

		registerMetaClass DefaultGrailsApplication
		mockOutDefaultGrailsApplication()
		grailsApplication = new DefaultGrailsApplication()

		mockOutSpringSecurityUtilsConfig()

		detailsServiceSettings = [
			authorityClassName: ROLE_CLASS_NAME,
			authorityJoinClassName: JOIN_CLASS_NAME,
			authorityNameField: "authority",
			samlAutoCreateActive: false,
			samlAutoCreateKey: true,
			samlUserAttributeMappings: [ username: USERNAME_ATTR_NAME ],
			samlUserGroupAttribute: GROUP_ATTR_NAME,
			samlUserGroupToRoleMapping: ['myGroup': ROLE],
			userDomainClassName: USER_CLASS_NAME,
			grailsApplication: grailsApplication ]

		nameID = new NameIDImpl("", "", "")
		assertion = new AssertionImpl("", "", "")

		stubTestRoleMethods()

		// This is what a SamlResponse will eventually be marshalled to
		credential = new SAMLCredential(nameID, assertion, null, null)
		credential.metaClass.getNameID = { [value: "$username"] }

		// set default username to be returned in the saml response
		setMockSamlAttributes(credential, ["$USERNAME_ATTR_NAME": username])

		registerMetaClass TestRole
		registerMetaClass TestUserRole
		TestUserRole.metaClass.'static'.removeAll = { TestSamlUser userWithRoles ->}
		TestUserRole.metaClass.'static'.create = { TestSamlUser userWithNoRoles, TestRole role ->}
		
		registerMetaClass TestSamlUser
	}

	void testServiceInstantiation() {
		def service = new SpringSamlUserDetailsService(detailsServiceSettings)
		assertNotNull service
	}
	
	void testUserInstanceClassIsGrailsUser() {

		def service = new SpringSamlUserDetailsService(detailsServiceSettings)

		def user = service.loadUserBySAML(credential)
		assert user instanceof GrailsUser
	}

	
	/**
	 * When no attribute mapping is specified for the username value then the NameID should be used 
	 */
	void testLoadUserWithSamlNameId() {

		def customSettings = detailsServiceSettings
		customSettings.samlUserAttributeMappings = [ username: null ]
		def service = new SpringSamlUserDetailsService(customSettings)

		setMockSamlAttributes(credential, ["$USERNAME_ATTR_NAME": "someotherValue"])

		def user = service.loadUserBySAML(credential)

		assert user.username == username
	}

	
	void testLoadUserUsernameAsAttribute() {

		def service = new SpringSamlUserDetailsService(detailsServiceSettings)

		def user = service.loadUserBySAML(credential)

		assert user.username == username
	}

	
	void testLoadUserUsernameAsAttributeUsernameNotSupplied() {

		def service = new SpringSamlUserDetailsService(detailsServiceSettings)

		setMockSamlAttributes(credential, ["$USERNAME_ATTR_NAME": null])

		try {
			def user = service.loadUserBySAML(credential)
			fail("Null username in saml response not handled correctly!")
			
		} catch (UsernameNotFoundException unfException) {
			assert unfException.message == "No username supplied in saml response."
			
		} catch (Exception ex) {
			fail("Unexpected exception raised.")
		}
	}
	

	void testAuthorityMappingsUserHasRole() {

		def service = new SpringSamlUserDetailsService(detailsServiceSettings)

		setMockSamlAttributes(credential, ["$GROUP_ATTR_NAME": "something=something,CN=myGroup", "$USERNAME_ATTR_NAME": 'myUsername'])

		def user = service.loadUserBySAML(credential)

		assert user.authorities.size() == 1
		assert user.authorities.toArray()[0].authority == ROLE
	}

	
	void testUserNotPersistedWhenNotActive() {

		def service = new SpringSamlUserDetailsService(detailsServiceSettings)

		mockDomain TestSamlUser, []

		assert TestSamlUser.count() == 0
		def userDetails = service.loadUserBySAML(credential)
		assert TestSamlUser.count() == 0
	}

	
	void testUserPersistedWhenFlaggedAndNotExists() {

		def customSettings = detailsServiceSettings
		customSettings.samlAutoCreateActive = true
		customSettings.samlAutoCreateKey = 'username'
		def service = new SpringSamlUserDetailsService(customSettings)

		mockDomain TestSamlUser, []
		stubTestSamlUserMethods()
		
		assert TestSamlUser.count() == 0
		def userDetails = service.loadUserBySAML(credential)

		assert TestSamlUser.count() == 1
		assert TestSamlUser.findByUsername(userDetails.username)
	}
	

	void testUserNotPersistedWhenFlaggedAndExists() {

		def customSettings = detailsServiceSettings
		customSettings.samlAutoCreateActive = true
		customSettings.samlAutoCreateKey = 'username'
		def service = new SpringSamlUserDetailsService(customSettings)

		def user = new TestSamlUser(username: username, password: 'test')
		mockDomain TestSamlUser, [user]
		stubTestSamlUserMethods(user)

		assert TestSamlUser.count() == 1
		def userDetail = service.loadUserBySAML(credential)
		
		assert TestSamlUser.count() == 1
	}

	
	void testUserNewUserAuthoritiesPersisted() {

		def customSettings = detailsServiceSettings
		customSettings.samlAutoCreateActive = true
		customSettings.samlAutoCreateKey = 'username'
		def service = new SpringSamlUserDetailsService(customSettings)
		
		setMockSamlAttributes(credential, ["$GROUP_ATTR_NAME": "something=something,CN=myGroup", "$USERNAME_ATTR_NAME": username])

		mockDomain TestSamlUser, []
		stubTestSamlUserMethods()

		TestUserRole.metaClass.'static'.removeAll = { TestSamlUser userWithRoles -> 
			// no roles to remove
			assert false
		}
		
		TestUserRole.metaClass.'static'.create = { TestSamlUser userWithNoRoles, TestRole role -> 
			assert userWithNoRoles.username == username
			assert role.authority == ROLE
		}
		
		def userDetail = service.loadUserBySAML(credential)
	}

	
	void testUserExistingUserAuthoritiesUpdated() {

		def customSettings = detailsServiceSettings
		customSettings.samlAutoCreateActive = true
		customSettings.samlAutoCreateKey = 'username'
		def service = new SpringSamlUserDetailsService(customSettings)

		mockDomain TestUserRole, []
		
		setMockSamlAttributes(credential, ["$GROUP_ATTR_NAME": "something=something,CN=myGroup", "$USERNAME_ATTR_NAME": username])

		def user = new TestSamlUser(username: username, password: 'test')
		mockDomain TestSamlUser, [user]
		stubTestSamlUserMethods(user)

		TestUserRole.metaClass.'static'.removeAll = { TestSamlUser userWithRoles ->
			assert userWithRoles.username == user.username
		}
		
		TestUserRole.metaClass.'static'.create = { TestSamlUser userWithNoRoles, TestRole role ->
			assert userWithNoRoles.username == user.username
			assert role.authority == ROLE
		}
		
		def userDetail = service.loadUserBySAML(credential)
	}
}
