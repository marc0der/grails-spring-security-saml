/* Copyright 2006-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.salenda.grails.plugins.springsecurity.saml

import org.opensaml.saml2.core.Attribute
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.security.saml.SAMLCredential
import org.springframework.security.saml.userdetails.SAMLUserDetailsService
import org.springframework.util.Assert
import org.codehaus.groovy.grails.plugins.springsecurity.GormUserDetailsService
import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser;
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.hibernate.Session

/**
 * A {@link GormUserDetailsService} extension to read attributes from a LDAP-backed 
 * SAML identity provider. It also reads roles from database
 * 
 * @author alvaro.sanchez
 */
class SpringSamlUserDetailsService extends GormUserDetailsService implements SAMLUserDetailsService {

	def sessionFactory
	def grailsApplication
	

	public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {

		def securityConfig = SpringSecurityUtils.securityConfig
		def username, fullName, email, password, authorities, user
		
		username = getSamlUsername(securityConfig, credential)
		password = ""
		
		log.debug "Loading database roles for $username..."
		authorities = getAuthoritiesForUser(securityConfig, username, credential)

		String userDomainClassName = securityConfig.userLookup.userDomainClassName

		Class<?> User = grailsApplication.getDomainClass(userDomainClassName).clazz
		user = BeanUtils.instantiateClass(User)
		user.username = username
		user.password = password
		
		createUserDetails(user, authorities)
	}
	
	protected String getSamlUsername(securityConfig, credential) {
		def userMappings = securityConfig.userAttributeMappings
		if (userMappings?.username) {
			return credential.getAttributeByName(userMappings.username)?.attributeValues?.value
			
		} else {
			// if no mapping provided for username attribute then assume it is the returned subject in the assertion
			return credential.nameID.value
		}
	}
	
	protected String getSamlAttribute(SAMLCredential credential, String attributeName) {
 		Attribute attribute = credential.getAttributeByName(attributeName)
		 
		def attributeValue = attribute.getAttributeValues()[0]
		
		attributeValue.value
	}
		
	protected GrailsUser mapSamlAttributes(credential, user, userAttributeMappings) {
		userAttributeMappings.each { userProperty, attributeName ->
			user."${userProperty}" = getSamlAttribute(credential, attributeName)
		}
		
		user
	}
	
	protected Collection<GrantedAuthority> getAuthoritiesForUser(ConfigObject securityConfig, String username, SAMLCredential credential) {
		Collection<GrantedAuthority> authorities = []

		def samlGroups = getSamlGroupsForUser(credential)
		log.debug "User is a member of saml groups: ${samlGroups}"
		
		Class<?> Role = grailsApplication.getDomainClass(securityConfig.authority.className).clazz
		
		def roles = []
		def groupToRoleMapping = SpringSecurityUtils.securityConfig.saml.userGroupToRoleMapping
		def authorityFieldName = securityConfig.authority.nameField
		samlGroups.each { groupName ->
			def role = groupToRoleMapping.get(groupName)
			def authority = Role.findWhere("$authorityFieldName":role)
			
			if (authority) {
				GrantedAuthority grantedAuthority = new GrantedAuthorityImpl(authority."$authorityFieldName")
				authorities << grantedAuthority
			} 
		}
		
		authorities
	}
	
	/**
	 * Return the groups that the user is a memberOf.  
	 * Expects the saml.userGroupAttribute to specify the saml assertion attribute that holds returned group membership data.
	 * 
	 * @param credential
	 * @return 
	 */
	protected List getSamlGroupsForUser(SAMLCredential credential) {
		def userGroups = []
		
		def groupAttribute = SpringSecurityUtils.securityConfig.saml.userGroupAttribute
		
		credential.getAttributeByName(groupAttribute).each { attribute ->
			attribute.getAttributeValues().each { attributeValue ->
				log.debug "Processing group attribute value: ${attributeValue.value}"
				
				def groupString = attributeValue.value
				
				groupString.tokenize(',').each { token ->
					def keyValuePair = token.tokenize('=')
					
					if (keyValuePair.first() == 'CN') {
						userGroups << keyValuePair.last()
					}
				}
			}
		}
		
		userGroups
	}
	
	
}
