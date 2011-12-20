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

import java.util.Collection

import org.codehaus.groovy.grails.plugins.springsecurity.GormUserDetailsService
import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.opensaml.saml2.core.Attribute
import org.springframework.beans.BeanUtils
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.saml.SAMLCredential
import org.springframework.security.saml.userdetails.SAMLUserDetailsService

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

		def conf = SpringSecurityUtils.securityConfig
		def username, authorities, user

		username = getSamlUsername(credential)
		if (!username) {
			throw new UsernameNotFoundException("No username supplied in saml response.")
		}
		
		user = generateSecurityUser(username)
		
		log.debug "Loading database roles for $username..."
		authorities = getAuthoritiesForUser(username, credential)

		createUserDetails(user, authorities)
	}

	protected String getSamlUsername(credential) {
		def conf = SpringSecurityUtils.securityConfig
		def userMappings = conf.saml.userAttributeMappings
		
		if (userMappings?.username) {

			def attribute = credential.getAttributeByName(userMappings.username)
			def value = attribute?.attributeValues?.value
			return value?.first()
		} else {
			// if no mapping provided for username attribute then assume it is the returned subject in the assertion
			return credential.nameID?.value
		}
	}

	protected Collection<GrantedAuthority> getAuthoritiesForUser(String username, SAMLCredential credential) {
		def conf = SpringSecurityUtils.securityConfig
		Collection<GrantedAuthority> authorities = []

		def samlGroups = getSamlGroups(credential)

		Class<?> Role = grailsApplication.getDomainClass(conf.authority.className).clazz
		def groupToRoleMapping = conf.saml.userGroupToRoleMapping
		def authorityFieldName = conf.authority.nameField

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
	 * Extract the groups that the user is a member of from the saml assertion.
	 * Expects the saml.userGroupAttribute to specify the saml assertion attribute that holds 
	 * returned group membership data.
	 * 
	 * Expects the group strings to be of the format "CN=groupName,someOtherParam=someOtherValue"
	 * 
	 * @param credential
	 * @return list of groups
	 */
	protected List getSamlGroups(SAMLCredential credential) {
		def userGroups = []

		def groupAttribute = SpringSecurityUtils.securityConfig.saml.userGroupAttribute
		if (groupAttribute) {
			def attributes = credential.getAttributeByName(groupAttribute)

			attributes.each { attribute ->
				attribute.attributeValues?.each { attributeValue ->
					log.debug "Processing group attribute value: ${attributeValue}"

					def groupString = attributeValue.value
					groupString.tokenize(',').each { token ->
						def keyValuePair = token.tokenize('=')

						if (keyValuePair.first() == 'CN') {
							userGroups << keyValuePair.last()
						}
					}
				}
			}
		}

		userGroups
	}

	private Object generateSecurityUser(username) {
		def conf = SpringSecurityUtils.securityConfig
		String userDomainClassName = conf.userLookup.userDomainClassName
		Class<?> UserClass = grailsApplication.getDomainClass(userDomainClassName).clazz
		def user = BeanUtils.instantiateClass(UserClass)
		user.username = username
		user.password = "password"
		
		saveUser(conf.saml.autoCreate, UserClass, user)
		
		user
	}
	
	private void saveUser(autoCreateConf, userClazz, user) {
		if (autoCreateConf?.active) {
			def userKey = autoCreateConf?.key
			def existingUser = userClazz.findWhere("$userKey": user."$userKey")
			
			if (!existingUser) {
				assert user.save()
			}
		}
	}
}
