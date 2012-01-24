security {
	saml {
		active = true
		afterLoginUrl = '/'
		afterLogoutUrl = '/'
		userGroupAttribute = "memberOf"
		autoCreate {
			active =  false
			key = 'username'
		}
		metadata {
			defaultIdp = 'ping'
			url = '/saml/metadata'
			providers = [ ping :'security/idp-local.xml']
			sp {
				file = 'security/sp.xml'
				defaults = [
					local: true, 
					alias: 'test',
					securityProfile: 'metaiop',
					signingKey: 'ping',
					encryptionKey: 'ping', 
					tlsKey: 'ping',
					requireArtifactResolveSigned: false,
					requireLogoutRequestSigned: false, 
					requireLogoutResponseSigned: false ]
			}
		}
		keyManager {
			storeFile = 'classpath:security/keystore.jks'
			storePass = 'nalle123'
			passwords = [ ping: 'ping123' ]
			defaultKey = 'ping'
		}
	}
}
