package es.salenda.grails.plugins.springsecurity.saml

import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.security.saml.SAMLCredential
import org.springframework.security.saml.userdetails.SAMLUserDetailsService
import org.codehaus.groovy.grails.plugins.springsecurity.GormUserDetailsService
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.hibernate.Session

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
		
		def conf = SpringSecurityUtils.securityConfig
		def joinClass = grailsApplication.getClassForName(conf.userLookup.authorityJoinClassName)
		def authorityPropertyName = conf.authority.nameField
		
		def session = sessionFactory.openSession()
		def roles = session.createQuery("from ${joinClass.name} where user=:user").setString('user', username).list()
		def authorities = roles.collect { new GrantedAuthorityImpl(it.role."$authorityPropertyName") }
		
		new SamlUser(username, password, true, true, true, true, authorities ?: NO_ROLES, fullName, email)
	}

}
