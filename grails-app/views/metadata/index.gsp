<!doctype html>
<html>
	<head>
		<meta name="layout" content="main"/>
		<title>Metadata</title>
	</head>
	<body>
		<div style="margin-left: 20px;">
		
			<h1>Login/Logout</h1>
			<sec:ifLoggedIn>
			     Welcome back, <sec:username/> | <sec:logoutLink local="true">Local logout</sec:logoutLink> | <sec:logoutLink>Global logout</sec:logoutLink>
			     <p>User has the following roles: <sec:ifAnyGranted roles="ROLE_USER">USER</sec:ifAnyGranted></p> 
			</sec:ifLoggedIn>
			
			
			
			<sec:ifNotLoggedIn>
				<sec:loginLink>Login</sec:loginLink> | <sec:loginLink selectIdp="true">Login (selecting IDP)</sec:loginLink> 
			</sec:ifNotLoggedIn>
		
			<h1>Metadata</h1>
			<p><g:link action="create">Generate new service provider metadata</g:link></p>
			<p>&nbsp;</p>
			
			<p>
			    Default hosted service provider: <br/>
		        <g:link action="show" params="[entityId:hostedSP]">${hostedSP}</g:link>
			    <br/>
			    <small><em>Default service provider is available without selection of alias.</em></small>
			</p>
			<p>&nbsp;</p>
			
			<p>
			    Service providers:<br/>
			    <g:each var="entity" in="${spList}">
			        <g:link action="show" params="[entityId:entity]">${entity}</g:link> <br/>
		        </g:each>
			    <br/>
			</p>	
	
			<p>
			    Identity providers:<br/>
			    <g:each var="entity" in="${idpList}">
			        <g:link action="show" params="[entityId:entity]">${entity}</g:link> <br/>
		        </g:each>
			    <br/>
			</p>	

		</div>		
	</body>
</html>		