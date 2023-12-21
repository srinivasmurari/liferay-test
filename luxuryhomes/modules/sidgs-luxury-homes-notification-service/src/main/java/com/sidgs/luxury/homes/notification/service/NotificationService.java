package com.sidgs.luxury.homes.notification.service;

/**
 * @author MuraliMohan
 */

public interface NotificationService{
	public void sendEmail(long groupId, String toEmailAddress, String password, String fullName, String emailTemplate);
	public void sendEmailAfterHosting(long groupId, String hostEmailAddress, String hostFullName, String propertyName, String address, String phone);
}