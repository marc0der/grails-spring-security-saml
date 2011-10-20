baseConfig = """
grails.plugins.springsecurity.saml.active = true
grails.plugins.springsecurity.saml.afterLoginUrl = '/'
grails.plugins.springsecurity.saml.afterLogoutUrl = '/'
grails.plugins.springsecurity.saml.metadata.defaultIdp = 'ping'
grails.plugins.springsecurity.saml.metadata.url = '/saml/metadata'
grails.plugins.springsecurity.saml.metadata.providers = ['ping':'security/idp-local.xml']
grails.plugins.springsecurity.saml.keyManager.storeFile = 'classpath:security/keystore.jks'
grails.plugins.springsecurity.saml.keyManager.storePass = 'nalle123'
grails.plugins.springsecurity.saml.keyManager.passwords = ['ping':'ping123']
grails.plugins.springsecurity.saml.keyManager.defaultKey = 'ping'
"""