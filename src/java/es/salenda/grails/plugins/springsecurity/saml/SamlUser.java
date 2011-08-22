package es.salenda.grails.plugins.springsecurity.saml;

import java.util.Collection;

import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser;
import org.springframework.security.core.GrantedAuthority;

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
