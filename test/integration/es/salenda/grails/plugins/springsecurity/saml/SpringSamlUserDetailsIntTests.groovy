
package es.salenda.grails.plugins.springsecurity.saml;

import static org.junit.Assert.*

import java.util.List;

import grails.test.GrailsUnitTestCase;
import groovy.util.GroovyTestCase
import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH
import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser;
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.impl.AssertionImpl;
import org.opensaml.saml2.core.impl.AttributeImpl;
import org.opensaml.saml2.core.impl.NameIDImpl;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

import test.TestSamlUser;

class SpringSamlUserDetailsIntTests extends GrailsUnitTestCase {
	
  def userDetailsService
  def grailsApplication
	
	/**
	* {@inheritDoc}
	* @see junit.framework.TestCase#setUp()
	*/
   @Override
   protected void setUp() {
	   super.setUp()
	   
	   mockLogging SpringSamlUserDetailsService
	   
	   CH.config = new ConfigObject()
//	   grailsApplication.addArtifact(TestSamlUser)
   }

   /**
	* {@inheritDoc}
	* @see junit.framework.TestCase#tearDown()
	*/
   @Override
   protected void tearDown() {
	   super.tearDown()
	   CH.config = null
   }
   
   void testIsSamlUserDetails() {
	   assert userDetailsService instanceof SAMLUserDetailsService
   }
   
   void testSamlUserUsernameSet() {
	   
//	   registerMetaClass SAMLCredential
//	   SAMLCredential.metaClass.getAttributeByName = { name ->
//	   		if (name == "")
//		   
//	   }
	   
	   registerMetaClass Attribute
	   Attribute.metaClass.getAttributeValues = {
			return [[value:'asdasda']].toArray()
	   }
	   
	   Attribute attribute = new AttributeImpl("", "", "")
	   attribute.name = "samlPasswordHash"
	   
	   def nameID = new NameIDImpl("", "", "")
	   nameID.value = "david"
	   
	   def attributes = [attribute]
	   def assertion = new AssertionImpl("", "", "")
	   def credential = new SAMLCredential(nameID, assertion, "", "", attributes, "")
	   
	   ConfigObject config = new ConfigObject()
	   def configMapping = [userAttributeMappings:[password:"samlPasswordHash"],
		   					userAuthorityAttribute: "group", 
							userLookup:[
								authorityJoinClassName: "test.TestSamlRole", 
								userDomainClassName: "test.TestSamlUser", 
								passwordPropertyName: "password", usernamePropertyName: "username"]
							]
	   
	   config.putAll(configMapping)
	   SpringSecurityUtils.setSecurityConfig(config)
	   
	   def samlUser = userDetailsService.loadUserBySAML(credential)
	   
	   assert samlUser instanceof TestSamlUser
	   assert samlUser.username == nameID.value  
   }
}
