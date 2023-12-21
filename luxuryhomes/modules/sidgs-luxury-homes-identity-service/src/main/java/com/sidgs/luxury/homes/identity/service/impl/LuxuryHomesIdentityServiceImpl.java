package com.sidgs.luxury.homes.identity.service.impl;

import com.liferay.expando.kernel.model.ExpandoTableConstants;
import com.liferay.expando.kernel.model.ExpandoValue;
import com.liferay.expando.kernel.service.ExpandoColumnLocalService;
import com.liferay.expando.kernel.service.ExpandoValueLocalService;
import com.liferay.oauth2.provider.exception.NoSuchOAuth2ApplicationException;
import com.liferay.oauth2.provider.model.OAuth2Application;
import com.liferay.oauth2.provider.service.OAuth2ApplicationLocalServiceUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.sidgs.luxury.homes.identity.headless.dto.v1_0.HospStatus;
import com.sidgs.luxury.homes.identity.headless.dto.v1_0.Register;
import com.sidgs.luxury.homes.identity.service.LuxuryHomesIdentityService;
import com.sidgs.luxury.homes.notification.service.NotificationService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, property = {}, service = LuxuryHomesIdentityService.class)
public class LuxuryHomesIdentityServiceImpl implements LuxuryHomesIdentityService{
	private final static Log _log = LogFactoryUtil.getLog(LuxuryHomesIdentityServiceImpl.class.getName());
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
	
	@Override
	public HospStatus registration(Register register) {
		HospStatus hospStatus = new HospStatus();
		hospStatus = validateRegisterObject(register);
		if(hospStatus.getStatus().equals("success")) {
			long companyId = CompanyThreadLocal.getCompanyId();
			User user = userLocalService.fetchUserByEmailAddress(companyId, register.getEmailAddress());
			if(user==null) {
				try {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			        LocalDate dobDate = LocalDate.parse(register.getDateOfBirth(), formatter);
			        int month = dobDate.getMonthValue()-1;
			        int day = dobDate.getDayOfMonth();
			        int year = dobDate.getYear();
					long guestUserId = userLocalService.getGuestUserId(CompanyThreadLocal.getCompanyId());
					ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();
					String password = StringUtil.randomString(8);
					user = userLocalService.addUser(guestUserId, companyId, false, password, password, true, null, register.getEmailAddress(), 
							LocaleUtil.US, register.getFirstName(), "", register.getLastName(), 0l, 0l, true, month, day, year, "", 1, new long[] {}, new long[] {}, new long[] {}, new long[] {}, true, serviceContext);
					_log.info("User created with email address ::"+register.getEmailAddress());
					userLocalService.updateAgreedToTermsOfUse(user.getUserId(), Boolean.TRUE);
					expandoValueLocalService.addValue(companyId, User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME,  "Phone Number", user.getUserId(), register.getPhoneNumber());
					try {
						Group defaultGroup = getDefaultGroup(companyId);
						if(defaultGroup!=null) {
							notificationService.sendEmail(defaultGroup.getGroupId(), user.getEmailAddress(), password, user.getFullName(), "registration");
						}
					}catch(Exception pe) {
						_log.error("Error while creating the User");
					}
					hospStatus.setStatus("success");
					hospStatus.setMessage("User Registration completed successfully");
				} catch (PortalException e) {
					hospStatus.setStatus("error");
					hospStatus.setMessage("User Registration Failed");
					_log.error("Error while creating the User");
				}
			}else {
				hospStatus.setStatus("error");
				hospStatus.setMessage("Account with this EmailAddress already exists");
			}
		}
		return hospStatus;
	}
	
	private HospStatus validateRegisterObject(Register register) {
		HospStatus hospStatus = new HospStatus();
		hospStatus.setStatus("error");
		boolean errStatus = Boolean.FALSE;
		if(register.getFirstName()==null || register.getFirstName().isEmpty()) {
			hospStatus.setMessage("First Name cannot be null / empty");
			errStatus = Boolean.TRUE;
		}
		if(register.getLastName()==null || register.getLastName().isEmpty()) {
			hospStatus.setMessage("Last Name cannot be null / empty");
			errStatus = Boolean.TRUE;
		}
		if(register.getEmailAddress()==null || register.getEmailAddress().isEmpty()) {
			hospStatus.setMessage("Email Address cannot be null / empty");
			errStatus = Boolean.TRUE;
		}
		boolean dobErrStatus = Boolean.FALSE;
		if(register.getDateOfBirth()==null || register.getDateOfBirth().isEmpty()) {
			hospStatus.setMessage("Date of Birth cannot be null / empty");
			dobErrStatus = Boolean.TRUE; errStatus = Boolean.TRUE;
		}
		if(!dobErrStatus) {
			try {
				dateFormat.parse(register.getDateOfBirth());
			} catch (ParseException e) {
				hospStatus.setMessage("Date format of Date of Birth should be dd-MM-yyyy");
				errStatus = Boolean.TRUE;
			}
		}
		boolean phErrStatus = Boolean.FALSE;
		if(register.getPhoneNumber()==null || register.getPhoneNumber().isEmpty()) {
			hospStatus.setMessage("Phone Number cannot be null / empty");
			phErrStatus = Boolean.TRUE; errStatus = Boolean.TRUE;
		}
		if(!phErrStatus) {
			List<String> existingPhoneNumbers = getAllCustomFieldValues("Phone Number");
			if(existingPhoneNumbers.contains(register.getPhoneNumber())) {
				hospStatus.setMessage("Phone Number already exists in the portal");
				errStatus = Boolean.TRUE;
			}
		}
		if(register.getTAndC()==null || !register.getTAndC()) {
			hospStatus.setMessage("Terms and Conditions should be checked");
			errStatus = Boolean.TRUE;
		}
		if(!errStatus) {
			hospStatus.setStatus("success");
		}
		return hospStatus;
	}
	
	private List<String> getAllCustomFieldValues(String customFieldName){
		List<String> phoneNumbersList = new ArrayList<String>();
		long companyId = CompanyThreadLocal.getCompanyId();
		List<ExpandoValue> columnValues = expandoValueLocalService.getColumnValues(companyId, User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME, "Phone Number", -1, -1);
		for(ExpandoValue columnValue:columnValues) {
			phoneNumbersList.add(columnValue.getData());
		}
		return phoneNumbersList;
	}

	@Override
	public boolean validateKeys(long companyId, String clientId, String secret) {
		try {
			OAuth2Application oAuth2Application = OAuth2ApplicationLocalServiceUtil.getOAuth2Application(companyId, clientId);
			String clientSecret = oAuth2Application.getClientSecret();
			if(clientSecret.equals(secret)) {
				return Boolean.TRUE;
			}
		} catch (NoSuchOAuth2ApplicationException e) {
			_log.error("No Such OAuth2Application with ClientId : "+clientId);
		}
		return Boolean.FALSE;
	}

	@Override
	public HospStatus forgotPassword(String emailAddressJSON) {
		HospStatus hospStatus = new HospStatus();
		hospStatus.setStatus("error");
		JSONObject obj = null;
		try {
			obj = JSONFactoryUtil.createJSONObject(emailAddressJSON);
		} catch (JSONException e) {
			_log.error("Exception while Parsing JSON Object");
		}
		if(obj!=null) {
			String emailAddress = obj.getString("emailAddress");
			if(!emailAddress.equals(StringPool.BLANK)) {
				long companyId = CompanyThreadLocal.getCompanyId();
				User user = userLocalService.fetchUserByEmailAddress(companyId, emailAddress);
				if(user!=null) {
					String password = StringUtil.randomString(8);
					try {
						userLocalService.updatePassword(user.getUserId(), password, password, Boolean.FALSE, Boolean.TRUE);
						Group defaultGroup = getDefaultGroup(companyId);
						if(defaultGroup!=null) {
							notificationService.sendEmail(defaultGroup.getGroupId(), user.getEmailAddress(), password, user.getFullName(), "forgot-password");
						}
						hospStatus.setStatus("success");
						hospStatus.setMessage("New password sent to your EmailAddress");
					} catch (PortalException e) {
						hospStatus.setMessage("Unable to reset password for EmailAddress :"+emailAddress);
						_log.error("Exception while setting password");
					}
				}else {
					hospStatus.setMessage("User doesn't exists in the portal");
				}
			}else {
				hospStatus.setMessage("Empty EmailAddress in JSONObject");
			}
		}else {
			hospStatus.setMessage("Not proper JSONObject");
		}
		return hospStatus;
	}
	
	private Group getDefaultGroup(long companyId) {
		Group defaultGroup = null;
		try {
			defaultGroup = groupLocalService.getFriendlyURLGroup(companyId, "/guest");
		} catch (PortalException e) {
			_log.error("Error while fetching default Group");
		}
		return defaultGroup;
	}

	@Reference
	UserLocalService userLocalService;
	@Reference
	GroupLocalService groupLocalService;
	@Reference
	NotificationService notificationService;
	@Reference
	ExpandoColumnLocalService expandoColumnLocalService;
	@Reference
	ExpandoValueLocalService expandoValueLocalService;
}
