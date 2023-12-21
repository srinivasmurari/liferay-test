package com.sidgs.luxury.homes.identity.headless.internal.resource.v1_0;


import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.sidgs.luxury.homes.identity.headless.dto.v1_0.HospStatus;
import com.sidgs.luxury.homes.identity.headless.dto.v1_0.Register;
import com.sidgs.luxury.homes.identity.headless.resource.v1_0.SignUpResource;
import com.sidgs.luxury.homes.identity.service.LuxuryHomesIdentityService;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author MuraliMohanSIDGlobal
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/sign-up.properties",
	scope = ServiceScope.PROTOTYPE, service = SignUpResource.class
)
public class SignUpResourceImpl extends BaseSignUpResourceImpl {
private final static Log log = LogFactoryUtil.getLog(SignUpResourceImpl.class.getName());
	
	@Override
	public Response postRegistration(Register register) throws Exception {
		HospStatus hospStatus = luxuryHomesIdentityService.registration(register);
		log.info("Registartion call completed ::"+hospStatus.getStatus());
		if(hospStatus.getStatus().equals("success")) {
			return Response.status(Status.OK).entity(hospStatus).build();
		}
		return Response.status(Status.CONFLICT).entity(hospStatus).build();
	}
	
	@Reference
	LuxuryHomesIdentityService luxuryHomesIdentityService;
}