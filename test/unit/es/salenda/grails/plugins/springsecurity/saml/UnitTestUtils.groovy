package es.salenda.grails.plugins.springsecurity.saml

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils

import test.TestRole
import test.TestSamlUser
import test.TestUserRole

class UnitTestUtils {
	static final ROLE = "ROLE_ADMIN"
	static final USER_CLASS_NAME = 'test.TestSamlUser'
	static final ROLE_CLASS_NAME = 'test.TestRole'
	static final JOIN_CLASS_NAME = 'test.TestUserRole'
	
	static final USERNAME_ATTR_NAME = 'usernameAttribute'
	static final GROUP_ATTR_NAME = 'groups'
	
	static ConfigObject setTestConfig() {
		def config = new ConfigObject()

		// set spring security core configuration and saml security config
		config.putAll([
					authority:[nameField:"authority", className: ROLE_CLASS_NAME],
					userLookup:[
						userDomainClassName: USER_CLASS_NAME,
						authorityJoinClassName: JOIN_CLASS_NAME,
						passwordPropertyName: "password",
						usernamePropertyName: "username",
						enabledPropertyName:"enabled",
						authoritiesPropertyName: "authorities",
						accountExpiredPropertyName: "accountExpired",
						accountLockedPropertyName: "accountLocked",
						passwordExpiredPropertyName: "passwordExpired"
					],
					saml:[userAttributeMappings:[username: USERNAME_ATTR_NAME]]
				])

		SpringSecurityUtils.metaClass.static.getSecurityConfig = { config }
		config
	}
	
	
	static void stubTestSamlUserMethods(user=null) {
		TestSamlUser.metaClass.'static'.findWhere = {user}
		TestSamlUser.metaClass.'static'.withTransaction = { Closure callable ->
			callable.call(null)
		}
	}
	
	/**
	* mock out DefaultGrailsApplication which is used to return grails domain class for a given name
	*/
   static void mockOutDefaultGrailsApplication() {
	   DefaultGrailsApplication.metaClass.getDomainClass { className ->
		   if (className == ROLE_CLASS_NAME) {
			   
			   return new DefaultGrailsDomainClass(TestRole.class, [:])
		   } else if (className == USER_CLASS_NAME) {
		   
			   return new DefaultGrailsDomainClass(TestSamlUser.class, [:])
		   } else if (className == JOIN_CLASS_NAME) {
		   
			   return new DefaultGrailsDomainClass(TestUserRole.class, [:])
		   }
		   return null
	   }
   }
   
   
   static void setMockSamlAttributes(credential, attributes=[:]) {
	   credential.metaClass.getAttributeByName = { String name ->
		   if ( name == USERNAME_ATTR_NAME) {
			   return [attributeValues: [
					   [value: attributes.get("${USERNAME_ATTR_NAME}")]
				   ]
			   ]
		   }
		   else if (name == GROUP_ATTR_NAME) {
			   return [
				   [attributeValues: [
						   [value: attributes.get("${GROUP_ATTR_NAME}")]
					   ]
				   ]
			   ]
		   }

		   return []
	   }
   }
}
