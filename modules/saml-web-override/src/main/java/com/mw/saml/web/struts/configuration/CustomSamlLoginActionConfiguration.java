package com.mw.saml.web.struts.configuration;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

import aQute.bnd.annotation.metatype.Meta;
import aQute.bnd.annotation.metatype.Meta.Type;

@ExtendedObjectClassDefinition(category = "custom-saml-login-action", scope = ExtendedObjectClassDefinition.Scope.SYSTEM)
@Meta.OCD(id = CustomSamlLoginActionConfiguration.PID, localization = "content/Language", name = "configuration.customSamlLoginAction.name", description="configuration.customSamlLoginAction.desc")
public interface CustomSamlLoginActionConfiguration {
	public static final String PID = "com.mw.saml.web.struts.configuration.CustomSamlLoginActionConfiguration";

	@Meta.AD(deflt = "false", required = false, type = Type.Boolean, name = "field.injectIdPEntityId.name", description = "field.injectIdPEntityId.desc")
	public boolean injectIdPEntityId();
	
	@Meta.AD(deflt = "", required = false, type = Type.String, name = "field.idPEntityId.name", description = "field.idPEntityId.desc")
	public String idPEntityId();
}