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
import java.util.Set

import org.codehaus.groovy.grails.plugins.springsecurity.GormUserDetailsService
import org.springframework.beans.BeanUtils
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.GrantedAuthorityImpl
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

	// Spring bean injected configuration parameters
	String authorityClassName
	String authorityJoinClassName
	String authorityNameField
	Boolean samlAutoCreateActive
	String samlAutoCreateKey
	Map samlUserAttributeMappings
	Map samlUserGroupToRoleMapping
	String samlUserGroupAttribute
	String userDomainClassName

	public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {

		if (credential) {
			String username = getSamlUsername(credential)
			if (!username) {
				throw new UsernameNotFoundException("No username supplied in saml response.")
			}

			def user = generateSecurityUser(username)
			if (user) {
				log.debug "Loading database roles for $username..."
				def authorities = getAuthoritiesForUser(credential)
				saveUser(user.class, user, authorities)
				createUserDetails(user, authorities)
			} else {
				throw new InstantiationException('could not instantiate new user')
			}
		}
	}

	protected String getSamlUsername(credential) {

		if (samlUserAttributeMappings?.username) {

			def attribute = credential.getAttributeByName(samlUserAttributeMappings.username)
			def value = attribute?.attributeValues?.value
			return value?.first()
		} else {
			// if no mapping provided for username attribute then assume it is the returned subject in the assertion
			return credential.nameID?.value
		}
	}

	protected Collection<GrantedAuthority> getAuthoritiesForUser(SAMLCredential credential) {
		Set<GrantedAuthority> authorities = new HashSet<GrantedAuthorityImpl>()

		def samlGroups = getSamlGroups(credential)

		samlGroups.each { groupName ->
			def role = samlUserGroupToRoleMapping.get(groupName)
			def authority = getRole(role)

			if (authority) {
				authorities.add( new GrantedAuthorityImpl(authority."$authorityNameField") )
			}
		}

		return authorities
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

		if (samlUserGroupAttribute) {
			def attributes = credential.getAttributeByName(samlUserGroupAttribute)

			attributes.each { attribute ->
				attribute.attributeValues?.each { attributeValue ->
					log.debug "Processing group attribute value: ${attributeValue}"

					def groupString = attributeValue.value
					groupString?.tokenize(',').each { token ->
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
		if (userDomainClassName) {
			Class<?> UserClass = grailsApplication.getDomainClass(userDomainClassName)?.clazz
			if (UserClass) {
				def user = BeanUtils.instantiateClass(UserClass)
				user.username = username
				user.password = "password"
				return user
			} else {
				throw new ClassNotFoundException("domain class ${userDomainClassName} not found")
			}
		} else {
				throw new ClassNotFoundException("security user domain class undefined")
		}
	}

	private void saveUser(userClazz, user, authorities) {

		if (userClazz && samlAutoCreateActive && samlAutoCreateKey 
				&& authorityNameField && authorityJoinClassName) {

			Map whereClause = [ "$samlAutoCreateKey": user."$samlAutoCreateKey" ]
			def existingUser = userClazz.findWhere(whereClause)

			Class<?> joinClass = grailsApplication.getDomainClass(authorityJoinClassName)?.clazz

			userClazz.withTransaction {
				if (!existingUser) {
					user.save()
				} else {
					user = existingUser
					joinClass.removeAll user
				}

				authorities.each { grantedAuthority ->
					def role = getRole(grantedAuthority."${authorityNameField}")
					joinClass.create(user, role)
				}
			}
		}
	}
	
	private Object getRole(String authority) {
		if (authority && authorityNameField && authorityClassName) {
			Class<?> Role = grailsApplication.getDomainClass(authorityClassName).clazz
			if ( Role ) {
				Map whereClause = [ "$authorityNameField": authority ]
				Role.findWhere(whereClause)
			} else {
				throw new ClassNotFoundException("domain class ${authorityClassName} not found")
			}
		}
	}
}
