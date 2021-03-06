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

import grails.plugins.springsecurity.SecurityTagLib
import org.springframework.security.saml.SAMLLogoutFilter
import org.codehaus.groovy.grails.commons.GrailsApplication

class SamlTagLib extends SecurityTagLib {

	static final String LOGOUT_SLUG = '/j_spring_security_logout'

	GrailsApplication grailsApplication

	/**
	 * {@inheritDocs}
	 */
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

	/**
	 * {@inheritDocs}
	 */
	def loginLink = { attrs, body ->
		def contextPath = request.contextPath
		def url = grailsApplication.config.grails.plugins.springsecurity.auth.loginFormUrl
		def selectIdp = attrs.remove('selectIdp')

		url = "${contextPath}${url}"
		if (!selectIdp) {
			def defaultIdp = grailsApplication.config.grails.plugins.springsecurity.saml.metadata.defaultIdp
			url += "?idp=${grailsApplication.config.grails.plugins.springsecurity.saml.metadata.providers[defaultIdp]}"
		}

		def elementClass = generateClassAttribute(attrs)
		def elementId = generateIdAttribute(attrs)

		out << "<a href='${url}'${elementId}${elementClass}>${body()}</a>"
	}

	/**
	 * {@inheritDocs}
	 */
	def logoutLink = { attrs, body ->
		def local = attrs.remove('local')
		def contextPath = request.contextPath

		def url = LOGOUT_SLUG

		def samlEnabled = grailsApplication.config.grails.plugins.springsecurity.saml.active
		if(samlEnabled){
			url = SAMLLogoutFilter.DEFAULT_FILTER_URL
		}

		def elementClass = generateClassAttribute(attrs)
		def elementId = generateIdAttribute(attrs)

		out << """<a href='${contextPath}${url}${local?'?local=true':''}'${elementId}${elementClass}>${body()}</a>"""
	}

	private String generateIdAttribute(Map attrs) {
		def elementId = ""
		if (attrs.id) {
			elementId = " id=\'${attrs.id}\'"
		}
		elementId
	}

	private String generateClassAttribute(Map attrs) {
		def elementClass = ""
		if (attrs.class) {
			elementClass = " class=\'${attrs.class}\'"
		}
		elementClass
	}
}
