<!doctype html>
<html>
	<head>
		<meta name="layout" content="main"/>
		<title>Metadata</title>
	</head>
	<body>
		<div style="margin-left: 20px;">
			<h1>Metadata - ${extendedMetadata.alias?:entityDescriptor.entityID}</h1>
			<p><a href="${request.contextPath}${grailsApplication.config.grails.plugins.springsecurity.saml.metadata.url}/${extendedMetadata.alias?'alias/'+extendedMetadata.alias:''}">Download Metadata</a></p>
			<br/>
			<h2>Properties</h2>
			<ul>
				<li><strong>Entity ID:</strong> ${entityDescriptor.entityID}</li>
				<li><strong>Alias:</strong> ${extendedMetadata.alias}</li>
				<li><strong>Security profile:</strong> ${extendedMetadata.securityProfile?:'MetaIOP'}</li>
				<li><strong>Signing key:</strong> ${extendedMetadata.signingKey}</li>
				<li><strong>Encryption key:</strong> ${extendedMetadata.encryptionKey}</li>
				<li><strong>SSL/TLS key:</strong> ${extendedMetadata.tlsKey}</li>
				<li><strong>Require signed LogoutRequest:</strong> ${extendedMetadata.requireLogoutRequestSigned}</li>
				<li><strong>Require signed LogoutResponse:</strong> ${extendedMetadata.requireLogoutResponseSigned}</li>
				<li><strong>Require signed ArtifactResolve:</strong> ${extendedMetadata.requireArtifactResolveSigned}</li>
			</ul>
			
			<br/>
			
			<g:if test="extendedMetadata.local">
				<h2>Local Entity</h2>
	            <p>In order to permanently store the metadata follow these instructions:</p>
	            <ol>
	                <li>
	                	Store metadata content in file <strong>${storagePath}</strong> and place in <strong>grails-app/conf/security/</strong> :<br/>
		                <textarea rows="15" cols="100" readonly="true" style="width:90%; font-size: 11px">${serializedMetadata}</textarea>
	                </li>
	                <li>Make sure to update your identity provider(s) with the generated metadata.</li>
	                <li>
						Update your Config.groovy and add the following lines:
						<p>
							grails.plugins.springsecurity.saml.metadata.sp.file = 'security/${storagePath}'
						</p>
						<p>
						grails.plugins.springsecurity.saml.metadata.sp.spMetadataDefaults =['local':true,'alias':'${extendedMetadata.alias}','securityProfile':'${extendedMetadata.securityProfile?:'MetaIOP'}','signingKey':'${extendedMetadata.signingKey}','encryptionKey':'${extendedMetadata.encryptionKey}','tlsKey':'${extendedMetadata.tlsKey}','requireArtifactResolveSigned':${extendedMetadata.requireArtifactResolveSigned},'requireLogoutRequestSigned':${extendedMetadata.requireLogoutRequestSigned}, 'requireLogoutResponseSigned':${extendedMetadata.requireLogoutResponseSigned}]
						</p>
	                </li>
	            </ol>			
			</g:if>
			
		</div>
	</body>
</html>		