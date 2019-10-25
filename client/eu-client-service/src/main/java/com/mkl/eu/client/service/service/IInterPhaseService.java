package com.mkl.eu.client.service.service;

import com.mkl.eu.client.common.exception.FunctionalException;
import com.mkl.eu.client.common.exception.TechnicalException;
import com.mkl.eu.client.common.vo.Request;
import com.mkl.eu.client.service.service.military.LandLootingRequest;
import com.mkl.eu.client.service.vo.diff.DiffResponse;

import javax.jws.WebParam;
import javax.jws.WebResult;

import static com.mkl.eu.client.service.service.INameConstants.PARAMETER_LAND_LOOTING;
import static com.mkl.eu.client.service.service.INameConstants.RESPONSE;

/**
 * I have no idea where to put all the methods of this service.
 * It gathers methods that will be called after the military phase
 * but before the peace segment.
 *
 * @author MKL.
 */
public interface IInterPhaseService {
    @WebResult(name = RESPONSE)
    DiffResponse landLooting(@WebParam(name = PARAMETER_LAND_LOOTING) Request<LandLootingRequest> request) throws FunctionalException, TechnicalException;

//    @WebResult(name = RESPONSE)
//    DiffResponse landRedeploy(@WebParam(name = PARAMETER_LAND_LOOTING) Request<LandLootingRequest> request) throws FunctionalException, TechnicalException;
//
//    @WebResult(name = RESPONSE)
//    DiffResponse validateRedeploy(@WebParam(name = PARAMETER_LAND_LOOTING) Request<LandLootingRequest> request) throws FunctionalException, TechnicalException;
//
//    @WebResult(name = RESPONSE)
//    DiffResponse exchequerRepartition(@WebParam(name = PARAMETER_LAND_LOOTING) Request<LandLootingRequest> request) throws FunctionalException, TechnicalException;
//
//    @WebResult(name = RESPONSE)
//    DiffResponse improveStability(@WebParam(name = PARAMETER_LAND_LOOTING) Request<LandLootingRequest> request) throws FunctionalException, TechnicalException;

}
