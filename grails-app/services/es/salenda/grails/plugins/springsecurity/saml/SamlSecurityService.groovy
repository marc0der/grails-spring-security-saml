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

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils;

import grails.plugins.springsecurity.SpringSecurityService

/**
 * A subclass of {@link SpringSecurityService} to replace {@link getCurrentUser()}
 * method. The parent implementation performs a database load, but we do not have
 * database users here, so we simply return the authentication details.
 * 
 * @author alvaro.sanchez
 */
class SamlSecurityService extends SpringSecurityService {

	static transactional = false

	Object getCurrentUser() {
		if (!isLoggedIn()) {
			return null
		}
		
		def userDetails = getAuthentication().details
		if ( SpringSecurityUtils.securityConfig.saml?.autoCreate?.active ) { 
			return getCurrentPersistedUser(userDetails)
		} 
		
		return userDetails
	}
	
	Object getCurrentPersistedUser() {
		def userDetails = getAuthentication().details
		if (userDetails) {
			def config = SpringSecurityUtils.securityConfig
			String className = config.userLookup.userDomainClassName
			String userKey = config.saml.autoCreate.key
			
			Class<?> userClass = grailsApplication.getDomainClass(className)?.clazz
			return userClass."findBy${userKey.capitalize()}"(userDetails."$userKey")
		} else { return null}
	}
}
