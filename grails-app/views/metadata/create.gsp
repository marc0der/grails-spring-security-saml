<!doctype html>
<html>
	<head>
		<meta name="layout" content="main"/>
		<title>Metadata</title>
		<style type="text/css">
			input {
				width: 500px;
			}
		</style>
	</head>
	<body>
		<div style="margin-left: 20px;">
			<h1>Generate metadata</h1>
			<p>
			    Generates a new metadata for service provider. Output can be used to configure your securityContext.xml descriptor.
			</p>
			
			<g:form action="save">
			    <table>
			
			        <tr>
			            <td>Store for the current session:</td>
			            <td>
			                <select name="store">
			                    <option value="true">Yes</option>
			                    <option value="false">No</option>
			                </select>
			                <br/>
			           		<small>When set to true the generated metadata will be stored in the local metadata manager. The value
			                    will be available
			                    only until restart of the application server.
			                </small>
			            </td>
			        </tr>
		
			        <tr>
			            <td>Entity ID:</td>
			            <td>
			            	<g:textField name="entityId"  value="${entityId}"/>
			            	<br/>
			                <small>Entity ID is a unique identifier for an identity or service provider. Value is included in the
			                    generated metadata.
			                </small>
			            </td>
			        </tr>

			        <tr>
			            <td>Entity base URL:</td>
			            <td>
			            	<g:textField name="baseURL" value="${baseUrl }"/>
			            	<br/>
			                <small>Base to generate URLs for this server. For example: https://myServer:443/saml-app. The public
			                    address your server will be accessed from should be used here.
			                </small>
			            </td>
			        </tr>

			        <tr>
			            <td>Entity alias:</td>
			            <td>
			            	<g:textField name="alias" value="${alias}"/>
			            	<br/>
			                <small>Alias is an internal mechanism allowing collocating multiple service providers on one server.
			                    Alias must be unique.
			                </small>
			            </td>
			        </tr>

			        <tr>
			            <td>Security profile:</td>
			            <td>
			                <select name="securityProfile">
			                    <option value="metaiop">MetaIOP</option>
			                    <option value="pkix">PKIX</option>
			                </select>
			                <br/>
			                <small>
			                    <p>Security profile determines how is trust of signature, encryption and SSL/TLS credentials handled. In
			                    MetaIOP mode credential is deemed valid when it's declared in the metadata document of the peer entity. No
			                    validation of the credentials is made. The value is recommended as a default.
			                    <p>PKIX profile verifies credentials against a set of trust anchors. By default certificates present in the
			                    metadata are treated as trust anchors together with the additional selected trusted keys.
			                </small>
			            </td>
			        </tr>

			
			        <tr>
			            <td>Signing key:</td>
			            <td>
			            	<g:select name="signingKey" from="${availableKeys}" optionKey="key" optionValue="value" />
			            	<br/>
			            	<small>Key used for digital signatures of SAML messages.</small>
			            </td>
			        </tr>

			        <tr>
			            <td>Encryption key:</td>
			            <td>
			            	<g:select name="encryptionKey" from="${availableKeys}" optionKey="key" optionValue="value" />
			            	<br/>
			                <small>Key used for digital encryption of SAML messages.</small>
			            </td>
			        </tr>

			        <tr>
			            <td>SSL/TLS key:</td>
			            <td>
			            	<g:select name="tlsKey" from="${availableKeys}" optionKey="key" optionValue="value" />
			            	<br/>
			                <small>Key used to authenticate this instance for SSL/TLS connections.</small>
			            </td>
			        </tr>
						
			        <tr>
			            <td>Sign metadata:</td>
			            <td>
			                <select name="signMetadata">
			                    <option value="true">Yes</option>
			                    <option value="false">No</option>
			                </select>
			                <br/>
			                <small>If true the generated metadata will be digitally signed using the specified signature key.
			                </small>
			            </td>
			        </tr>

			
			        <tr>
			            <td>Sign sent AuthNRequests:</td>
			            <td>
			                <select name="requestSigned">
			                    <option value="true">Yes</option>
			                    <option value="false">No</option>
			                </select>
			            </td>
			        </tr>
			        <tr>
			            <td>Require signed authentication Assertion:</td>
			            <td>
			                <select name="wantAssertionSigned">
			                    <option value="true">Yes</option>
			                    <option value="false">No</option>
			                </select>
			            </td>
			        </tr>
			        <tr>
			            <td>Require signed LogoutRequest:</td>
			            <td>
			                <select name="requireLogoutRequestSigned">
			                    <option value="true">Yes</option>
			                    <option value="false">No</option>
			                </select>
			            </td>
			        </tr>
			        <tr>
			            <td>Require signed LogoutResponse:</td>
			            <td>
			                <select name="requireLogoutResponseSigned">
			                    <option value="true">Yes</option>
			                    <option value="false">No</option>
			                </select>
			            </td>
			        </tr>
			        <tr>
			            <td>Require signed ArtifactResolve:</td>
			            <td>
			                <select name="requireArtifactResolveSigned">
			                    <option value="true">Yes</option>
			                    <option value="false">No</option>
			                </select>
			            </td>
			        </tr>
			
			        <tr>
			            <td colspan="2">
			                <br/>
			                <input type="submit" value="Generate metadata"/>
			            </td>
			        </tr>
			        
			    </table>
			</g:form>
			
		</div>
	</body>
</html>		