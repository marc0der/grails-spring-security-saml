security {
	saml {
		active = true
		userAttributeMappings = [password: "password"]
		userGroupAttribute = "memberOf"
		userGroupToRoleMapping = ['GRG.APP.DigitalCatalogue':"ROLE_USER"]
		entryPoint {
			//idpSelectionPath = '/saml/idpSelection.gsp'
		}
		metadata{
			url = '/saml/metadata'
			providers = [
				// TODO : Would be better if this was a classpath ref
				'ping':'security/idp-local.xml'
			]
			defaultIdp = 'ssoSSCircle'
		}
		keyManager {
			storeFile = "classpath:security/keystore.jks"
			storePass = "nalle123"
			passwords = ['ping':'ping123']
			defaultKey = 'ping'
		}
					
		afterLoginUrl = '/metadata'
		afterLogoutUrl = '/metadata'
	}
}