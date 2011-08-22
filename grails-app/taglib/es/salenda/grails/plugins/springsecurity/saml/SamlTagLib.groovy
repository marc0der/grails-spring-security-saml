package es.salenda.grails.plugins.springsecurity.saml

import grails.plugins.springsecurity.SecurityTagLib
import org.springframework.security.saml.SAMLLogoutFilter

class SamlTagLib extends SecurityTagLib {

	def loggedInUserInfo = { attrs, body ->
		String field = assertAttribute('field', attrs, 'loggedInUserInfo')

		def source = springSecurityService.authentication.details."${field}"

		if (source) {
			out << source.encodeAsHTML()
		}
		else {
			out << body()
		}
	}
	
	def loginLink = { attrs, body ->
		def contextPath = request.contextPath
		def url = grailsApplication.config.grails.plugins.springsecurity.auth.loginFormUrl
		def selectIdp = attrs.remove('selectIdp')
		
		url = "${contextPath}${url}"
		if (!selectIdp) {
			def defaultIdp = grailsApplication.config.grails.plugins.springsecurity.saml.metadata.defaultIdp
			url += "?idp=${grailsApplication.config.grails.plugins.springsecurity.saml.metadata.providers[defaultIdp]}"
		}
		
		out << "<a href='${url}'>${body()}</a>"
	}
	
	def logoutLink = { attrs, body ->
		def local = attrs.remove('local')
		def contextPath = request.contextPath
		def url = SAMLLogoutFilter.DEFAULT_FILTER_URL
		
		out << "<a href='${contextPath}${url}${local?'?local=true':''}'>${body()}</a>"
	}

}
