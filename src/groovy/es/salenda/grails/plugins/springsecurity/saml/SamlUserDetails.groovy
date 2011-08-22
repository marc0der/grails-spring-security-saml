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

import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.security.saml.SAMLCredential
import org.springframework.security.saml.userdetails.SAMLUserDetailsService
import org.springframework.util.Assert
import org.codehaus.groovy.grails.plugins.springsecurity.GormUserDetailsService
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.hibernate.Session

/**
 * A {@link GormUserDetailsService} extension to read attributes from a LDAP-backed 
 * SAML identity provider. It also reads roles from database
 * 
 * @author alvaro.sanchez
 */
class SamlUserDetails extends GormUserDetailsService implements SAMLUserDetailsService {

	def sessionFactory

	@Override
	public Object loadUserBySAML(SAMLCredential credential)
	throws UsernameNotFoundException {

		def username, fullName, email, password, user

		credential.attributes.each {
			if (it.name == 'uid') username = it.attributeValues[0].value
			else if (it.name == 'cn') fullName = it.attributeValues[0].value
			else if (it.name == 'mail') email = it.attributeValues[0].value
			else if (it.name == 'userPassword') password = it.attributeValues[0].value
		}

		Assert.notNull (username, "Username is required")
		Assert.notNull (fullName, "Full Name is required")

		log.debug "Loading database roles for $username..."
		def conf = SpringSecurityUtils.securityConfig
		def joinClass = grailsApplication.getClassForName(conf.userLookup.authorityJoinClassName)
		def authorityPropertyName = conf.authority.nameField

		// Need to manually open an Hibernate session as no session is bound
		// here at this point.
		def session = sessionFactory.openSession()
		def roles = session.createQuery("from ${joinClass.name} where user=:user").setString('user', username).list()
		def authorities = roles.collect { new GrantedAuthorityImpl(it.role."$authorityPropertyName") }

		new SamlUser(username, password, true, true, true, true, authorities ?: NO_ROLES, fullName, email)
	}

}
