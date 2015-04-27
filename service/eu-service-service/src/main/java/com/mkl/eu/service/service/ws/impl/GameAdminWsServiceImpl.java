package com.mkl.eu.service.service.ws.impl;

import com.mkl.eu.client.service.service.IGameAdminService;
import com.mkl.eu.client.service.vo.diff.DiffResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.jws.WebService;

/**
 * Separation from GameService because cxf can't handle @Transactional.
 *
 * @author MKL.
 */
@WebService(endpointInterface = "com.mkl.eu.client.service.service.IGameAdminService")
public class GameAdminWsServiceImpl extends SpringBeanAutowiringSupport implements IGameAdminService {
    /** Game Service. */
    @Autowired
    @Qualifier(value = "gameAdminServiceImpl")
    private IGameAdminService gameAdminService;

    /** {@inheritDoc} */
    @Override
    public DiffResponse moveStack(Long idGame, Long versionGame, Long idStack, String provinceTo) {
        return gameAdminService.moveStack(idGame, versionGame, idStack, provinceTo);
    }
}