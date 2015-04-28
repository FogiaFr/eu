package com.mkl.eu.service.service.persistence.country.impl;

import com.mkl.eu.service.service.persistence.country.IPlayableCountryDao;
import com.mkl.eu.service.service.persistence.impl.GenericDaoImpl;
import com.mkl.eu.service.service.persistence.oe.country.PlayableCountryEntity;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

/**
 * Implementation of the PlayableCountry DAO.
 *
 * @author MKL.
 */
@Repository
public class PlayableCountryDaoImpl extends GenericDaoImpl<PlayableCountryEntity, Long> implements IPlayableCountryDao {
    /**
     * Constructor.
     */
    public PlayableCountryDaoImpl() {
        super(PlayableCountryEntity.class);
    }

    /** {@inheritDoc} */
    @Override
    public PlayableCountryEntity getCountryByName(String name, Long idGame) {
        Criteria criteria = getSession().createCriteria(PlayableCountryEntity.class);

        criteria.add(Restrictions.eq("name", name));
        criteria.add(Restrictions.eq("game.id", idGame));

        return (PlayableCountryEntity) criteria.uniqueResult();
    }
}