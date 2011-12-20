

package es.salenda.grails.plugins.springsecurity.saml

import grails.test.GrailsUnitTestCase
import groovy.util.ConfigObject

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.opensaml.saml2.core.impl.AssertionImpl
import org.opensaml.saml2.core.impl.NameIDImpl
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.saml.*

import test.TestRole
import test.TestSamlUser


class SpringSamlUserDetailsServiceTest extends GrailsUnitTestCase {
	def userDetailsService, credential, nameID, assertion, mockGrailsAplication, config
	def userClassName = 'test.TestUser'
	def roleClassName = 'test.TestRole'
	def groupAttributeName = 'groups'
	def usernameAttributeName = 'usernameAttribute'
	def username = "jackSparrow"

	@Override
	protected void setUp() {
		super.setUp()

		userDetailsService = new SpringSamlUserDetailsService()
		
		nameID = new NameIDImpl("", "", "")
		assertion = new AssertionImpl("", "", "")
		
		// This is what a SamlResponse will eventually be marshalled to
		credential = new SAMLCredential(nameID, assertion, null, null)

		mockOutDefaultGrailsApplication()
		userDetailsService.grailsApplication = new DefaultGrailsApplication()

		// set default username to be returned in the saml response
		setMockSamlAttributes(["$usernameAttributeName": username])
		
		setTestConfig()
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
		setMockSamlAttributes(["$usernameAttributeName": "someotherValue"])
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
		setMockSamlAttributes(["$usernameAttributeName": null])

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
		def role = "ROLE_ADMIN"

		def additionalConfig = [saml:
			[userGroupAttribute: groupAttributeName, userGroupToRoleMapping: ['myGroup': role], 
				userAttributeMappings:[username: usernameAttributeName]]
		]
		config.putAll additionalConfig
		
		registerMetaClass TestRole
		// mockDomain does not support findWhere until grails 1.4
		TestRole.metaClass.static.findWhere = {new TestRole(authority: role)}
		
		setMockSamlAttributes(["$groupAttributeName": "something=something,CN=myGroup",
					"$usernameAttributeName": 'myUsername'])

		def user = userDetailsService.loadUserBySAML(credential)

		assert user.authorities.size() == 1
		assert user.authorities.toArray()[0].authority == role
	}

	void testUserPersistedWhenFlaggedAndNotExists() {
		mockDomain TestSamlUser, []

		registerMetaClass TestSamlUser
		TestSamlUser.metaClass.static.findWhere = {}

		def additionalConfig = [saml:[autoCreate: [active: true, key: 'username'], 
			userAttributeMappings:[username: usernameAttributeName]]
		]
		config.putAll additionalConfig

		assert TestSamlUser.count() == 0

		def userDetails = userDetailsService.loadUserBySAML(credential)

		assert TestSamlUser.count() == 1
		assert TestSamlUser.findByUsername(userDetails.username)
	}

	void testUserNotPersistedWhenFlaggedAndExists() {
		def user = new TestSamlUser(username: username, password: 'test')
		mockDomain TestSamlUser, [user]

		registerMetaClass TestSamlUser
		TestSamlUser.metaClass.static.findWhere = {   return user   }

		def additionalConfig = [saml:[autoCreate: [active: true, key: 'username'], 
			userAttributeMappings:[username: usernameAttributeName]]
		]
		config.putAll additionalConfig

		assert TestSamlUser.count() == 1
		
		def userDetail = userDetailsService.loadUserBySAML(credential)
		assert TestSamlUser.count() == 1
	}

	/**
	 * Helper methods
	 */

	/**
	 * mock out DefaultGrailsApplication which is used to return grails domain class for a given name  
	 */
	private void mockOutDefaultGrailsApplication() {
		registerMetaClass DefaultGrailsApplication
		DefaultGrailsApplication.metaClass.getDomainClass { className ->
			if (className == roleClassName) {
				return new DefaultGrailsDomainClass(TestRole.class, [:])
			} else if (className == userClassName) {
				return new DefaultGrailsDomainClass(TestSamlUser.class, [:])
			}
			return null
		}
	}

	private setMockSamlAttributes(attributes=[:]) {
		credential.metaClass.getAttributeByName = { String name ->
			if ( name == usernameAttributeName) {
				return [attributeValues: [
						[value: attributes.get("$usernameAttributeName")]
					]
				]
			}
			else if (name == groupAttributeName) {
				return [
					[attributeValues: [
							[value: attributes.get("$groupAttributeName")]
						]
					]
				]
			}

			return []
		}
	}

	private setTestConfig() {
		config = new ConfigObject()
		
		// set spring security core configuration and saml security config
		config.putAll([
					authority:[nameField:"authority", className: roleClassName],
					userLookup:[
						userDomainClassName: userClassName,
						passwordPropertyName: "password",
						usernamePropertyName: "username",
						enabledPropertyName:"enabled",
						authoritiesPropertyName: "authorities",
						accountExpiredPropertyName: "accountExpired",
						accountLockedPropertyName: "accountLocked",
						passwordExpiredPropertyName: "passwordExpired"
					],
					saml:[userAttributeMappings:[username: usernameAttributeName]]
				])

		SpringSecurityUtils.metaClass.static.getSecurityConfig = { config }
	}
}