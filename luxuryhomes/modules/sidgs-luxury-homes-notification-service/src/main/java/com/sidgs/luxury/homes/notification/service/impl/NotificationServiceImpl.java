package com.sidgs.luxury.homes.notification.service.impl;

import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.mail.kernel.model.MailMessage;
import com.liferay.mail.kernel.service.MailServiceUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.sidgs.luxury.homes.notification.service.NotificationService;

import javax.mail.internet.InternetAddress;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, property = {}, service = NotificationService.class)
public class NotificationServiceImpl implements NotificationService{
	private final static Log log = LogFactoryUtil.getLog(NotificationServiceImpl.class.getName());
	@Override
	public void sendEmail(long groupId, String toEmailAddress, String password, String fullName, String emailTemplate) {
		try {
			String fromEmailAddress = PropsUtil.get("hosp.email.from.address");
			JournalArticle article = journalArticleLocalService.fetchArticleByUrlTitle(groupId, emailTemplate);
			if(article!=null) {
				JSONObject articleJson = JSONFactoryUtil.createJSONObject(JSONFactoryUtil.convertXMLtoJSONMLObject(article.getContent()));
				String contentSubject = getFieldFromArticle(articleJson, "Subject");
				log.info("Subject ::"+contentSubject);
				String contentMailBody = getFieldFromArticle(articleJson, "EmailBody");
				log.info("Body ::"+contentMailBody);
				contentMailBody = StringUtil.replace(contentMailBody,
						new String[] { "[$FULLNAME$]", "[$PASSWORD$]", "[$EMAILADDRESS$]"},
						new String[] { fullName, password, toEmailAddress});
				InternetAddress fromInternetAddress = new InternetAddress(fromEmailAddress, "SID Luxury Homes");
				InternetAddress toInternetAddress = new InternetAddress(toEmailAddress);
				MailMessage mailMessage = new MailMessage();
				mailMessage.setTo(toInternetAddress);
				mailMessage.setFrom(fromInternetAddress);
				mailMessage.setSubject(contentSubject);
				mailMessage.setBody(contentMailBody);
				mailMessage.setHTMLFormat(true);
				MailServiceUtil.sendEmail(mailMessage);
				log.info("Email send from "+fromEmailAddress+" to "+toEmailAddress);
			}
		}catch(Exception e) {
			log.error(e);
		}
	}
	
	@Override
	public void sendEmailAfterHosting(long groupId, String hostEmailAddress, String hostFullName, String propertyName, String address, String phone) {
		String consolidatedAddress = StringPool.BLANK;
		try {
			JSONObject addObj = JSONFactoryUtil.createJSONObject(address);
			consolidatedAddress = addObj.getString("PlotNo") + StringPool.COMMA + addObj.getString("Locality") + StringPool.COMMA + addObj.getString("City") 
			+ StringPool.COMMA + addObj.getString("State") + StringPool.COMMA + addObj.getString("Country") + StringPool.COMMA + addObj.getString("Zipcode");
		} catch (JSONException e1) {
			log.error("Error while parsing the Address Obj");
		}
		try {
			String adminEmailAddress = PropsUtil.get("hosp.email.from.address");
			JournalArticle article = journalArticleLocalService.fetchArticleByUrlTitle(groupId, "property-hosted-acknowledgement");
			if(article!=null) {
				JSONObject articleJson = JSONFactoryUtil.createJSONObject(JSONFactoryUtil.convertXMLtoJSONMLObject(article.getContent()));
				String contentSubject = getFieldFromArticle(articleJson, "Subject");
				log.info("Subject ::"+contentSubject);
				String contentMailBody = getFieldFromArticle(articleJson, "EmailBody");
				contentMailBody = StringUtil.replace(contentMailBody,
						new String[] { "[$FULLNAME$]", "[$PROPERTYNAME$]", "[$PROPERTYOWNERNAME$]", "[$ADDRESS$]", "[$PROPERTYOWNEREMAIL$]", "[$PROPERTYOWNERPHONE$]"},
						new String[] { hostFullName, propertyName, hostFullName, consolidatedAddress, hostEmailAddress, phone});
				log.debug("Body ::"+contentMailBody);
				sendEmail(adminEmailAddress, hostEmailAddress, adminEmailAddress, "SIDGS Luxury Homes", contentSubject, contentMailBody);
			}
			article = journalArticleLocalService.fetchArticleByUrlTitle(groupId, "property-hosted-information");
			if(article!=null) {
				JSONObject articleJson = JSONFactoryUtil.createJSONObject(JSONFactoryUtil.convertXMLtoJSONMLObject(article.getContent()));
				String contentSubject = getFieldFromArticle(articleJson, "Subject");
				log.info("Subject ::"+contentSubject);
				String contentMailBody = getFieldFromArticle(articleJson, "EmailBody");
				contentMailBody = StringUtil.replace(contentMailBody,
						new String[] { "[$FULLNAME$]", "[$PROPERTYNAME$]", "[$PROPERTYOWNERNAME$]", "[$ADDRESS$]", "[$PROPERTYOWNEREMAIL$]", "[$PROPERTYOWNERPHONE$]"},
						new String[] { "Admin", propertyName, hostFullName, consolidatedAddress, hostEmailAddress, phone});
				log.debug("Body ::"+contentMailBody);
				sendEmail(adminEmailAddress, adminEmailAddress, hostEmailAddress, hostFullName, contentSubject, contentMailBody);
			}
		}catch(Exception e) {
			log.error(e);
		}
	}
	
	private void sendEmail(String adminEmailAddress, String toEmailAddress, String byEmailAddress, String byFullName, String subject, String mailBody) {
		try {
			InternetAddress fromInternetAddress = new InternetAddress(adminEmailAddress, byEmailAddress, byFullName);
			InternetAddress toInternetAddress = new InternetAddress(toEmailAddress);
			MailMessage mailMessage = new MailMessage();
			mailMessage.setTo(toInternetAddress);
			mailMessage.setFrom(fromInternetAddress);
			mailMessage.setSubject(subject);
			mailMessage.setBody(mailBody);
			mailMessage.setHTMLFormat(true);
			MailServiceUtil.sendEmail(mailMessage);
		}catch(Exception e) {
			log.error(e);
		}
	}
	
	private final String getFieldFromArticle(JSONObject json, String fieldName) {
		try {
			JSONArray childNodes = json.getJSONArray("childNodes");
			for (int i = 0; i < childNodes.length(); i++) {
				JSONObject o = childNodes.getJSONObject(i);
				log.debug(o.getString("field-reference"));
				if (o.getString("field-reference").equals(fieldName)) {
					log.debug(o.toJSONString());
					JSONArray nodes = o.getJSONArray("childNodes");
					String val = nodes.getJSONObject(0).getJSONArray("childNodes").getString(0);
					log.debug(val);
					return val;
				}
			}
		} catch (Exception e) {
			log.error(e);
		}
		log.debug("not found");
		return "";
	}
	
	@Reference
	JournalArticleLocalService journalArticleLocalService;
}
