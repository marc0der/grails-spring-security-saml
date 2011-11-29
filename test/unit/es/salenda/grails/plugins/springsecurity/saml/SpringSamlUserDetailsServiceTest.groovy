

package es.salenda.grails.plugins.springsecurity.saml

import grails.test.GrailsUnitTestCase
import grails.test.GrailsUnitTestCase
import groovy.util.ConfigObject

import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser
import org.codehaus.groovy.grails.plugins.springsecurity.ReflectionUtils
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.opensaml.saml2.core.impl.AssertionImpl
import org.opensaml.saml2.core.impl.NameIDImpl
import org.springframework.security.saml.*
import org.springframework.security.core.userdetails.UsernameNotFoundException

import test.TestRole
import test.TestSamlUser;


class SpringSamlUserDetailsServiceTest extends GrailsUnitTestCase {
	def userDetailsService, credential, nameID, assertion, mockGrailsAplication
	def userClassName = 'test.TestUser'
	def roleClassName = 'test.TestRole'
 	def groupAttributeName = 'groups'
	def usernameAttributeName = 'usernameAttribute'

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() {
		super.setUp()
		
		registerMetaClass SAMLCredential
		nameID = new NameIDImpl("", "", "")
		assertion = new AssertionImpl("", "", "")
		credential = new SAMLCredential(nameID, assertion, null, null)

		userDetailsService = new SpringSamlUserDetailsService()

		mockOutDefaultGrailsApplication()
		userDetailsService.grailsApplication = new DefaultGrailsApplication()
		
		setTestConfig()
	}

	/**
	 * {@inheritDoc}
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() {
		super.tearDown()
	}

	void testUserInstanceClass() {
		credential.metaClass.getNameID = { [value:"jackSparrow"] }

		def user = userDetailsService.loadUserBySAML(credential)
		assert user instanceof GrailsUser
	}
	
	void testLoadUserUsernameWithSamlNameId() {
		def username = "jackSparrow"

		credential.metaClass.getNameID = { [value:"$username"] }

		def user = userDetailsService.loadUserBySAML(credential)
		
		assert user.username == username
	}

	void testLoadUserUsernameAsAttribute() {
		def username = "jackSparrow"
		
		def additionalConfig = [saml:[userAttributeMappings:[username: usernameAttributeName]]]
		setTestConfig(additionalConfig)
		
		setMockSamlAttributes(["$usernameAttributeName": username])
		
		def user = userDetailsService.loadUserBySAML(credential)
		assert user.username == username
	}

	void testLoadUserUsernameAsAttributeUsernameNotSupplied() {
		def additionalConfig = [saml:[userAttributeMappings:[username: usernameAttributeName]]]
		setTestConfig(additionalConfig)
		
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
		
		registerMetaClass TestRole
		// mockDomain does not support findWhere until grails 1.4 
		TestRole.metaClass.static.findWhere = {new TestRole(authority: role)}
		
		def additionalConfig = [saml:
			[userGroupAttribute: groupAttributeName, userGroupToRoleMapping: ['myGroup': role], 
				userAttributeMappings:[username: usernameAttributeName]
			]
		]		
		setTestConfig(additionalConfig)
		setMockSamlAttributes(["$groupAttributeName": "something=something,CN=myGroup", 
			"$usernameAttributeName": 'myUsername'])

		def user = userDetailsService.loadUserBySAML(credential)
		
		assert user.authorities.size() == 1
		assert user.authorities.toArray()[0].authority == role
	}

	/**
	 * Helper methods
	 */

	private mockOutDefaultGrailsApplication() {
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
				return [attributeValues: [[
				 	value: attributes.get("$usernameAttributeName")
				]]]
			} 
			else if (name == groupAttributeName) {
				return [[attributeValues: [[
					value: attributes.get("$groupAttributeName")
				]]]]
			}

			return []
		}
	}

	private setTestConfig(additionalConfig=[:]) {
		def config = new ConfigObject()

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
						]
					])
		
		config.putAll additionalConfig
		
		SpringSecurityUtils.metaClass.static.getSecurityConfig = { config }
	}
}