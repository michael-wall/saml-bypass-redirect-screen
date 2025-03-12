## Introduction ##
- This custom OSGi module is a POC to bypass the usual 'Redirecting to your identity provider...' screen when an SP initiated SAML Login is triggered.
- This module overrides the out of the box SamlLoginAction.java class. It injects the IdP Entity ID to bypass the  'Redirecting to your identity provider...' screen.
- The custom logic in the CustomSamlLoginAction.java class is disabled by default, there is a custom configuration setting to toggle it on or off and to set the IdP Entity ID to inject in.

## Environment ##
- The OSGi module is targeted at Liferay DXP 7.4 U92.
- It should be compiled with JDK 11 (or JDK 8).
- This customization is suitable for Liferay self hosted and Liferay PaaS. It is not suitable for Liferay SaaS as it is an OSGi extension.

## Setup ##
1. Add the modules to the Liferay PaaS repository within Liferay/modules.
2. Allow the Liferay PaaS build to be generated and deploy the build to the target environment.
3. Login as an Administrator, go to Control Panel > System Settings > Security > Custom SAML User Resolver, enable 'Inject IDP Entity ID', populate 'IDP Entity ID' and Save.
4. Go to System Settings > Platform > Module Container > Component Blacklist, add 'com.liferay.saml.web.internal.struts.SamlLoginAction' (without quotes) and Save / Update.
5. Test the scenario of accessing a projected resource and confirm that the usual 'Redirecting to your identity provider...' screen isn't displayed and that the user is able to login via SAML sucessfully.

## Configuration ##
- The following configuration file can be used in place of enabling / disabling through the System Settings GUI.
- For Liferay PaaS the file can be added to the liferay service in the DXP Cloud repository e.g. liferay/configs/dev/osgi/configs
- File name: com.mw.saml.web.struts.configuration.CustomSamlLoginActionConfiguration.config
- File content to enable:

```
idPEntityId="....."
injectIdPEntityId=B"true"
```

(where ..... is the plain text IdP Entity ID)

## Notes ##
1. This is a public repository and is provided as a Proof Of Cencept (POC), as is. 
2. The POC assumes that 'System Settings > Login > Prompt Enabled' and 'Instance Settings > Login > Prompt Enabled' setting enabled.
3. The POC assumes that the auth.login.url portal property (or the corresponding LIFERAY_AUTH_PERIOD_LOGIN_PERIOD_URL Environment Variable) is NOT set in the environment. Permanently remove if present.
4. The POC assumes there is only 1 IdP defined per Liferay Environment.
5. When upgrading to a later Liferay DXP / Quarterly Release version ensure that the code is updated (if needed) to reflect any changes in SamlLoginAction.java.
6. Source for Liferay DXP 7.4 U92 SamlLoginAction.java is here: https://github.com/liferay/liferay-dxp/blob/7.4.13-u92/modules/dxp/apps/saml/saml-web/src/main/java/com/liferay/saml/web/internal/struts/SamlLoginAction.java
