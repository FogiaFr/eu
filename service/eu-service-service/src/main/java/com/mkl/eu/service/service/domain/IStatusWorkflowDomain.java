package com.mkl.eu.service.service.domain;

import com.mkl.eu.service.service.persistence.oe.GameEntity;
import com.mkl.eu.service.service.persistence.oe.diff.DiffEntity;

import java.util.List;

/**
 * Interface for cross service methods around status workflow.
 *
 * @author MKL.
 */
public interface IStatusWorkflowDomain {
    /**
     * Compute the end of the administrative actions phase.
     * It will then switch to minor logistics phase except
     * if no minor is involved in a war, in which case it will
     * call computeEndMinorLogistics.
     *
     * @param game the game.
     * @return the diffs related to the change of phase.
     */
    List<DiffEntity> computeEndAdministrativeActions(GameEntity game);

    /**
     * Compute the end of the minor logistics phase.
     * It will then switch to military phase, and creates
     * the turn order accordingly.
     *
     * @param game the game.
     * @return the diffs related to the change of phase.
     */
    List<DiffEntity> computeEndMinorLogistics(GameEntity game);

    /**
     * Compute the end of a round.
     * Go to next round, or next phase if it was the last round.
     *
     * @param game the game.
     * @return the diffs related to the change of round.
     */
    List<DiffEntity> nextRound(GameEntity game);
}