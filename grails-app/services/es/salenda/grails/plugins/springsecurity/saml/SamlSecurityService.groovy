package es.salenda.grails.plugins.springsecurity.saml

import grails.plugins.springsecurity.SpringSecurityService

class SamlSecurityService extends SpringSecurityService {

	static transactional = false

	Object getCurrentUser() {
		if (!isLoggedIn()) {
			return null
		}
		getAuthentication().details
	}

}
