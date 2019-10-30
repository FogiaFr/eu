package com.mkl.eu.service.service.ws.impl;

import com.mkl.eu.client.common.exception.FunctionalException;
import com.mkl.eu.client.common.exception.TechnicalException;
import com.mkl.eu.client.common.vo.Request;
import com.mkl.eu.client.service.service.IInterPhaseService;
import com.mkl.eu.client.service.service.military.LandLootingRequest;
import com.mkl.eu.client.service.vo.diff.DiffResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.jws.WebService;

/**
 * Separation from InterPhaseService because cxf can't handle @Transactional.
 *
 * @author MKL.
 */
@WebService(endpointInterface = "com.mkl.eu.client.service.service.IInterPhaseService")
public class InterPhaseWsServiceImpl extends SpringBeanAutowiringSupport implements IInterPhaseService {
    /** Interphase Service. */
    @Autowired
    @Qualifier(value = "interPhaseServiceImpl")
    private IInterPhaseService interPhaseService;

    /** {@inheritDoc} */
    @Override
    public DiffResponse landLooting(Request<LandLootingRequest> request) throws FunctionalException, TechnicalException {
        return interPhaseService.landLooting(request);
    }
}