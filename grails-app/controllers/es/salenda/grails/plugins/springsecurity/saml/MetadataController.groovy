package es.salenda.grails.plugins.springsecurity.saml

import java.security.KeyStoreException

import org.springframework.security.saml.metadata.ExtendedMetadata
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate
import org.springframework.security.saml.metadata.MetadataMemoryProvider
import org.springframework.security.core.context.SecurityContextHolder
import org.w3c.dom.Element
import org.opensaml.Configuration
import org.opensaml.saml2.metadata.EntityDescriptor
import org.opensaml.saml2.metadata.provider.MetadataProvider
import org.opensaml.saml2.metadata.provider.MetadataProviderException
import org.opensaml.xml.io.Marshaller
import org.opensaml.xml.io.MarshallerFactory
import org.opensaml.xml.io.MarshallingException
import org.opensaml.xml.security.credential.Credential
import org.opensaml.xml.util.XMLHelper

import grails.plugins.springsecurity.Secured


class MetadataController {

	def metadataGenerator
	def metadata
	def keyManager
	
	def index() {
		[hostedSP: metadata.hostedSPName, spList: metadata.SPEntityNames, idpList:metadata.IDPEntityNames]
	}

	def show() {
		def entityDescriptor = metadata.getEntityDescriptor(params.entityId)
		def extendedMetadata = metadata.getExtendedMetadata(params.entityId)
		def storagePath = getFileName(entityDescriptor)
		def serializedMetadata = getMetadataAsString(entityDescriptor)

		[entityDescriptor: entityDescriptor, extendedMetadata: extendedMetadata, 
			storagePath: storagePath, serializedMetadata: serializedMetadata]
	}
	
	def create() {
		def availableKeys = getAvailablePrivateKeys()
		def baseUrl = "${request.scheme}://${request.serverName}:${request.serverPort}${request.contextPath}"
		
		log.debug("Server name used as entity id and alias")
		def entityId = request.serverName
		def alias = entityId
		
		[availableKeys: availableKeys, baseUrl: baseUrl, entityId: entityId, alias: alias]
	}
	
	def save() {
		
        metadataGenerator.setEntityId(params.entityId)
        metadataGenerator.setEntityAlias(params.alias)
        metadataGenerator.setEntityBaseURL(params.baseURL)
        metadataGenerator.setSignMetadata(params.signMetadata as boolean)
        metadataGenerator.setRequestSigned(params.requestSigned as boolean)
        metadataGenerator.setWantAssertionSigned(params.wantAssertionSigned as boolean)
        metadataGenerator.setSigningKey(params.signingKey)
        metadataGenerator.setEncryptionKey(params.encryptionKey)
        metadataGenerator.setTlsKey(params.tlsKey)

		def descriptor = metadataGenerator.generateMetadata()
		
		def extendedMetadata = new ExtendedMetadata()
		metadataGenerator.generateExtendedMetadata(extendedMetadata)
		extendedMetadata.setSecurityProfile(params.securityProfile)
		extendedMetadata.setRequireLogoutRequestSigned(params.requireLogoutRequestSigned as boolean)
		extendedMetadata.setRequireLogoutResponseSigned(params.requireLogoutResponseSigned as boolean)
		extendedMetadata.setRequireArtifactResolveSigned(params.requireArtifactResolveSigned as boolean)

		if (params.store) {
			MetadataMemoryProvider memoryProvider = new MetadataMemoryProvider(descriptor)
			memoryProvider.initialize()
			MetadataProvider metadataProvider = new ExtendedMetadataDelegate(memoryProvider, extendedMetadata)
			metadata.addMetadataProvider(metadataProvider)
			metadata.setHostedSPName(descriptor.entityID)
			metadata.setRefreshRequired(true)
			metadata.refreshMetadata()
		}
		
		redirect(action:'show', params:[entityId: params.entityId])
	}

	protected def getFileName(entityDescriptor) {
		StringBuilder fileName = new StringBuilder()
		for (Character c : entityDescriptor.getEntityID().toCharArray()) {
			if (Character.isJavaIdentifierPart(c)) {
				fileName.append(c)
			}
		}
		if (fileName.length() > 0) {
			fileName.append("_sp.xml")
			fileName.toString()
		} else {
			"default_sp.xml"
		}
	}

	protected def getMetadataAsString(entityDescriptor) throws MarshallingException {
		MarshallerFactory marshallerFactory = Configuration.getMarshallerFactory()
		Marshaller marshaller = marshallerFactory.getMarshaller(entityDescriptor)
		Element element = marshaller.marshall(entityDescriptor)
		return XMLHelper.nodeToString(element)
	}
	
	protected def getAvailablePrivateKeys() throws KeyStoreException {
		Map<String, String> availableKeys = new HashMap<String, String>()
		Set<String> aliases = keyManager.getAvailableCredentials()
		for (String key : aliases) {
			try {
				Credential credential = keyManager.getCredential(key)
				if (credential.getPrivateKey() != null) {
					availableKeys.put(key, key + " (" + credential.getEntityId() + ")")
				}
			} catch (Exception e) {
				log.debug("Error loading key: ${e}")
			}
		}
		availableKeys
	}
	
}
