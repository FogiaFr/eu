package com.mkl.eu.service.service.persistence.diff.board.impl;

import com.mkl.eu.service.service.persistence.diff.board.IProvinceDao;
import com.mkl.eu.service.service.persistence.impl.GenericDaoImpl;
import com.mkl.eu.service.service.persistence.oe.board.AbstractProvinceEntity;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

/**
 * Implementation of the Game DAO.
 *
 * @author MKL.
 */
@Repository
public class ProvinceDaoImpl extends GenericDaoImpl<AbstractProvinceEntity, Long> implements IProvinceDao {
    /**
     * Constructor.
     */
    public ProvinceDaoImpl() {
        super(AbstractProvinceEntity.class);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractProvinceEntity getProvinceByName(String name) {
        Criteria criteria = getSession().createCriteria(AbstractProvinceEntity.class);

        criteria.add(Restrictions.eq("name", name));

        return (AbstractProvinceEntity) criteria.uniqueResult();
    }
}