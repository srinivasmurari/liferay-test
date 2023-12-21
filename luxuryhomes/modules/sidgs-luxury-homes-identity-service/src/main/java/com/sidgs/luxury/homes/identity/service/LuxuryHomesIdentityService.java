package com.sidgs.luxury.homes.identity.service;

import com.sidgs.luxury.homes.identity.headless.dto.v1_0.HospStatus;
import com.sidgs.luxury.homes.identity.headless.dto.v1_0.Register;

/**
 * @author MuraliMohan
 */
public interface LuxuryHomesIdentityService{

	public HospStatus registration(Register register);
	public boolean validateKeys(long companyId, String clientId, String secret);
	public HospStatus forgotPassword(String emailAddressJSON);

}