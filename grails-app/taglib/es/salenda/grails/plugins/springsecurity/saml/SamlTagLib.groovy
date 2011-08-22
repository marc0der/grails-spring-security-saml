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

class SamlTagLib extends SecurityTagLib {

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

		out << "<a href='${url}'>${body()}</a>"
	}

	/**
	 * {@inheritDocs}
	 */
	def logoutLink = { attrs, body ->
		def local = attrs.remove('local')
		def contextPath = request.contextPath
		def url = SAMLLogoutFilter.DEFAULT_FILTER_URL

		out << "<a href='${contextPath}${url}${local?'?local=true':''}'>${body()}</a>"
	}
}
