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
security {
	saml {
		active = true
		entryPoint {
			//idpSelectionPath = '/saml/idpSelection.gsp'
		}
		metadata{
			url = '/saml/metadata'
			providers = [
				'ssoCircle':'http://idp.ssocircle.com/idp-meta.xml'
			]
			defaultIdp = 'ssoCircle'
		}
		keyManager {
			storeFile = "classpath:security/samlKeystore.jks"
			storePass = "nalle123"
			passwords = ['apollo':'nalle123']
			defaultKey = 'apollo'
		}
		afterLoginUrl = '/'
		afterLogoutUrl = '/'
	}
}
