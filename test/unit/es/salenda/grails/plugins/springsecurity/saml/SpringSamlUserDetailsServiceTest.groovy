package es.salenda.grails.plugins.springsecurity.saml

import static es.salenda.grails.plugins.springsecurity.saml.UnitTestUtils.*

import grails.test.GrailsUnitTestCase
import groovy.util.ConfigObject

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser
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
	def userDetailsService, credential, nameID, assertion, mockGrailsAplication, config

	def username = "jackSparrow"

	@Override
	protected void setUp() {
		super.setUp()

		userDetailsService = new SpringSamlUserDetailsService()

		nameID = new NameIDImpl("", "", "")
		assertion = new AssertionImpl("", "", "")

		// This is what a SamlResponse will eventually be marshalled to
		credential = new SAMLCredential(nameID, assertion, null, null)

		registerMetaClass DefaultGrailsApplication
		mockOutDefaultGrailsApplication()
		userDetailsService.grailsApplication = new DefaultGrailsApplication()

		// set default username to be returned in the saml response
		setMockSamlAttributes(credential, ["$USERNAME_ATTR_NAME": username])

		config = setTestConfig()
		
		registerMetaClass TestRole
		registerMetaClass TestUserRole
		TestUserRole.metaClass.'static'.removeAll = { TestSamlUser userWithRoles ->}
		TestUserRole.metaClass.'static'.create = { TestSamlUser userWithNoRoles, TestRole role ->}
		
		registerMetaClass TestSamlUser
		
	}

	
	void testUserInstanceClassIsGrailsUser() {
		credential.metaClass.getNameID = { [value:"jackSparrow"] }

		def user = userDetailsService.loadUserBySAML(credential)
		assert user instanceof GrailsUser
	}

	
	/**
	 * When no attribute mapping is specified for the username value then the NameID should be used 
	 */
	void testLoadUserWithSamlNameId() {
		setMockSamlAttributes(credential, ["$USERNAME_ATTR_NAME": "someotherValue"])
		def additionalConfig = [saml:[userAttributeMappings:[username: null]]]
		config.putAll additionalConfig

		credential.metaClass.getNameID = { [value:"$username"] }

		def user = userDetailsService.loadUserBySAML(credential)

		assert user.username == username
	}

	
	void testLoadUserUsernameAsAttribute() {
		def user = userDetailsService.loadUserBySAML(credential)

		assert user.username == username
	}

	
	void testLoadUserUsernameAsAttributeUsernameNotSupplied() {
		setMockSamlAttributes(credential, ["$USERNAME_ATTR_NAME": null])

		try {
			def user = userDetailsService.loadUserBySAML(credential)
			fail("Null username in saml response not handled correctly!")
			
		} catch (UsernameNotFoundException unfException) {
			assert unfException.message == "No username supplied in saml response."
			
		} catch (Exception ex) {
			fail("Unexpected exception raised.")
		}
	}
	

	void testAuthorityMappingsUserHasRole() {
		def additionalConfig = [saml:[
			userGroupAttribute: GROUP_ATTR_NAME, userGroupToRoleMapping: ['myGroup': ROLE],
			userAttributeMappings:[username: USERNAME_ATTR_NAME]
		]]
		config.putAll additionalConfig

		
		setMockSamlAttributes(credential, ["$GROUP_ATTR_NAME": "something=something,CN=myGroup", "$USERNAME_ATTR_NAME": 'myUsername'])
		TestRole.metaClass.static.findWhere = {new TestRole(authority: ROLE)}

		def user = userDetailsService.loadUserBySAML(credential)

		assert user.authorities.size() == 1
		assert user.authorities.toArray()[0].authority == ROLE
	}

	
	void testUserNotPersistedWhenNotActive() {
		mockDomain TestSamlUser, []

		assert TestSamlUser.count() == 0
		def userDetails = userDetailsService.loadUserBySAML(credential)
		assert TestSamlUser.count() == 0
	}

	
	void testUserPersistedWhenFlaggedAndNotExists() {
		stubTestSamlUserMethods()
		mockDomain TestSamlUser, []
		
		def additionalConfig = [saml:[
			autoCreate: [active: true, key: 'username'],
			userAttributeMappings:[username: USERNAME_ATTR_NAME]
		]]
		config.putAll additionalConfig

		assert TestSamlUser.count() == 0
		def userDetails = userDetailsService.loadUserBySAML(credential)

		assert TestSamlUser.count() == 1
		assert TestSamlUser.findByUsername(userDetails.username)
	}
	

	void testUserNotPersistedWhenFlaggedAndExists() {
		def additionalConfig = [saml: [
			autoCreate: [active: true, key: 'username'],
			userAttributeMappings:[username: USERNAME_ATTR_NAME]
		]]
		config.putAll additionalConfig

		def user = new TestSamlUser(username: username, password: 'test')
		mockDomain TestSamlUser, [user]
		stubTestSamlUserMethods(user)

		assert TestSamlUser.count() == 1
		def userDetail = userDetailsService.loadUserBySAML(credential)
		
		assert TestSamlUser.count() == 1
	}

	
	void testUserNewUserAuthoritiesPersisted() {
		mockDomain TestUserRole, []
		def additionalConfig = [saml: [
			userGroupAttribute: GROUP_ATTR_NAME, userGroupToRoleMapping: ['myGroup': ROLE],
			autoCreate: [active: true, key: 'username'], userAttributeMappings: [username: USERNAME_ATTR_NAME]
		]]
		config.putAll additionalConfig
		
		TestRole.metaClass.static.findWhere = {new TestRole(authority: ROLE)}
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
		
		def userDetail = userDetailsService.loadUserBySAML(credential)
	}

	
	void testUserExistingUserAuthoritiesUpdated() {
		mockDomain TestUserRole, []
		def additionalConfig = [saml: [
			userGroupAttribute: GROUP_ATTR_NAME, userGroupToRoleMapping: ['myGroup': ROLE],
			autoCreate: [active: true, key: 'username'], userAttributeMappings: [username: USERNAME_ATTR_NAME]
		]]
		config.putAll additionalConfig
		
		TestRole.metaClass.static.findWhere = {new TestRole(authority: ROLE)}
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
		
		def userDetail = userDetailsService.loadUserBySAML(credential)
	}
}