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
package es.salenda.grails.plugins.springsecurity.saml;

import java.util.Collection;

import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser;
import org.springframework.security.core.GrantedAuthority;

/**
 * A {@link GrailsUser} subclass to add email and full name 
 * @author alvaro.sanchez
 *
 */
public class SamlUser extends GrailsUser{
	
	private static final long serialVersionUID = -4051398912660521656L;
	
	final String email;
	final String fullName;

	public SamlUser(String username, String password, boolean enabled,
			boolean accountNonExpired, boolean credentialsNonExpired,
			boolean accountNonLocked,
			Collection<GrantedAuthority> authorities,
			String fullName, String email) {
		
		super(username, password, enabled, accountNonExpired, credentialsNonExpired,
				accountNonLocked, authorities, -1);
		
		this.email = email;
		this.fullName = fullName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(this.fullName);
		String result = sb.append(" (").append(getUsername()).append(")").toString();
		return result;
	}


}
