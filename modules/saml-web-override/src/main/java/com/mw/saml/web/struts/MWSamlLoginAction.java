package com.mw.saml.web.struts;

import com.liferay.osgi.util.service.Snapshot;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.AuthTokenUtil;
import com.liferay.portal.kernel.struts.StrutsAction;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Props;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.saml.constants.SamlWebKeys;
import com.liferay.saml.persistence.model.SamlSpIdpConnection;
import com.liferay.saml.persistence.service.SamlSpIdpConnectionLocalService;
import com.liferay.saml.runtime.configuration.SamlProviderConfiguration;
import com.liferay.saml.runtime.configuration.SamlProviderConfigurationHelper;
import com.liferay.saml.runtime.servlet.profile.SamlSpIdpConnectionsProfile;
import com.liferay.saml.util.JspUtil;
import com.liferay.saml.web.internal.struts.BaseSamlStrutsAction;
import com.liferay.saml.web.internal.struts.SamlLoginAction;
import com.mw.saml.web.struts.configuration.CustomSamlLoginActionConfiguration;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Stian Sigvartsen
 */
@Component(
	property = "path=/portal/saml/login",
	service = StrutsAction.class,
	configurationPid = CustomSamlLoginActionConfiguration.PID)
public class MWSamlLoginAction extends BaseSamlStrutsAction {
	
	@Activate
	@Modified
    protected void activate(Map<String, Object> properties) throws Exception {		
		if (_log.isInfoEnabled()) _log.info("Activate...");		
		
		_customSamlLoginActionConfiguration = ConfigurableUtil.createConfigurable(CustomSamlLoginActionConfiguration.class, properties);
		
		_log.info("injectIdPEntityId: " + _customSamlLoginActionConfiguration.injectIdPEntityId());
		
		_log.info("idPEntityId: " + _customSamlLoginActionConfiguration.idPEntityId());
	}	
		

	@Override
	public boolean isEnabled() {
		if (_samlProviderConfigurationHelper.isRoleSp()) {
			return _samlProviderConfigurationHelper.isEnabled();
		}

		return false;
	}

	@Override
	protected String doExecute(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws Exception {

		_log.info("doExecute");
		
		String entityId = ParamUtil.getString(
			httpServletRequest, "idpEntityId");
		
		if (_customSamlLoginActionConfiguration.injectIdPEntityId() && Validator.isNotNull(_customSamlLoginActionConfiguration.idPEntityId())) {
			if (Validator.isNull(entityId)) {
				
				entityId = _customSamlLoginActionConfiguration.idPEntityId();
				
				_log.info("idPEntityId: " + entityId + " injected in...");
			}
		}

		long companyId = _portal.getCompanyId(httpServletRequest);

		if (Validator.isNotNull(entityId)) {
			httpServletRequest.setAttribute(
				SamlWebKeys.SAML_SP_IDP_CONNECTION,
				_samlSpIdpConnectionLocalService.getSamlSpIdpConnection(
					companyId, entityId));

			if (GetterUtil.getBoolean(
					ParamUtil.getBoolean(httpServletRequest, "forceAuthn"))) {

				AuthTokenUtil.checkCSRFToken(
					httpServletRequest, SamlLoginAction.class.getName());

				httpServletRequest.setAttribute(
					SamlWebKeys.FORCE_REAUTHENTICATION, Boolean.TRUE);
			}

			return null;
		}

		List<SamlSpIdpConnection> samlSpIdpConnections = ListUtil.filter(
			_samlSpIdpConnectionLocalService.getSamlSpIdpConnections(companyId),
			samlSpIdpConnection -> isEnabled(
				samlSpIdpConnection, httpServletRequest));

		if (samlSpIdpConnections.isEmpty()) {
			SamlProviderConfiguration samlProviderConfiguration =
				_samlProviderConfigurationHelper.getSamlProviderConfiguration();

			if (samlProviderConfiguration.allowShowingTheLoginPortlet()) {
				return null;
			}
		}

		boolean samlIdpRedirectMessageEnabled = GetterUtil.getBoolean(
			_props.get("saml.idp.redirect.message.enabled"), true);

		if (samlIdpRedirectMessageEnabled) {
			httpServletRequest.setAttribute(
				SamlWebKeys.SAML_IDP_REDIRECT_MESSAGE,
				_language.get(
					httpServletRequest,
					"redirecting-to-your-identity-provider"));
		}

		httpServletRequest.setAttribute(
			SamlWebKeys.SAML_SSO_LOGIN_CONTEXT,
			_toJSONObject(samlSpIdpConnections));

		JspUtil.dispatch(
			httpServletRequest, httpServletResponse,
			"/portal/saml/select_idp.jsp",
			"please-select-your-identity-provider",
			!samlIdpRedirectMessageEnabled);

		return null;
	}

	protected boolean isEnabled(
		SamlSpIdpConnection samlSpIdpConnection,
		HttpServletRequest httpServletRequest) {

		SamlSpIdpConnectionsProfile samlSpIdpConnectionsProfile =
			_samlSpIdpConnectionsProfileSnapshot.get();

		if (samlSpIdpConnectionsProfile != null) {
			return samlSpIdpConnectionsProfile.isEnabled(
				samlSpIdpConnection, httpServletRequest);
		}

		return samlSpIdpConnection.isEnabled();
	}

	private JSONObject _toJSONObject(
		List<SamlSpIdpConnection> samlSpIdpConnections) {

		JSONArray jsonArray = _jsonFactory.createJSONArray();

		for (SamlSpIdpConnection samlSpIdpConnection : samlSpIdpConnections) {
			jsonArray.put(
				JSONUtil.put(
					"enabled", samlSpIdpConnection.isEnabled()
				).put(
					"entityId", samlSpIdpConnection.getSamlIdpEntityId()
				).put(
					"name", samlSpIdpConnection.getName()
				));
		}

		return JSONUtil.put("relevantIdpConnections", jsonArray);
	}

	private static final Snapshot<SamlSpIdpConnectionsProfile>
		_samlSpIdpConnectionsProfileSnapshot = new Snapshot<>(
			SamlLoginAction.class, SamlSpIdpConnectionsProfile.class, null,
			true);

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private Language _language;

	@Reference
	private Portal _portal;

	@Reference
	private Props _props;

	@Reference
	private SamlProviderConfigurationHelper _samlProviderConfigurationHelper;

	@Reference
	private SamlSpIdpConnectionLocalService _samlSpIdpConnectionLocalService;
	
	private static final Log _log = LogFactoryUtil.getLog(MWSamlLoginAction.class);

	private volatile CustomSamlLoginActionConfiguration _customSamlLoginActionConfiguration;		
}