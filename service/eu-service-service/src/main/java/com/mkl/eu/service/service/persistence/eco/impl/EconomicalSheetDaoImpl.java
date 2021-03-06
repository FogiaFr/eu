package com.mkl.eu.service.service.persistence.eco.impl;

import com.mkl.eu.client.common.util.CommonUtil;
import com.mkl.eu.client.service.vo.enumeration.CounterFaceTypeEnum;
import com.mkl.eu.service.service.persistence.eco.IEconomicalSheetDao;
import com.mkl.eu.service.service.persistence.impl.GenericDaoImpl;
import com.mkl.eu.service.service.persistence.oe.eco.EconomicalSheetEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the EconomicalSheet DAO.
 *
 * @author MKL.
 */
@Repository
public class EconomicalSheetDaoImpl extends GenericDaoImpl<EconomicalSheetEntity, Long> implements IEconomicalSheetDao {
    /** Template jdbc . */
    @Autowired
    private JdbcTemplate jdbcTemplate;
    /** Sql queries. */
    @Autowired
    @Qualifier("queryProps")
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Properties queryProps;

    /**
     * Constructor.
     */
    public EconomicalSheetDaoImpl() {
        super(EconomicalSheetEntity.class);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Integer> getOwnedAndControlledProvinces(String name, Long idGame) {
        Map<String, Integer> incomeByProvinces = new HashMap<>();

        String sql = queryProps.getProperty("income.NationalProvinces");

        sql = sql.replace(":countryName", name);
        sql = sql.replace(":idGame", Long.toString(idGame));
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        results.stream().forEach(input -> incomeByProvinces.put((String) input.get("PROVINCE"), (Integer) input.get("INCOME")));

        sql = queryProps.getProperty("income.OwnedProvinces");

        sql = sql.replace(":countryName", name);
        sql = sql.replace(":idGame", Long.toString(idGame));
        results = jdbcTemplate.queryForList(sql);

        results.stream().forEach(input -> incomeByProvinces.put((String) input.get("PROVINCE"), (Integer) input.get("INCOME")));

        return incomeByProvinces;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getPillagedProvinces(List<String> provinces, Long idGame) {
        List<String> pillagedProvinces = new ArrayList<>();

        String sql = queryProps.getProperty("income.PillagedProvinces");

        String provinceNames = provinces.stream().collect(Collectors.joining("','", "('", "')"));
        sql = sql.replace(":provinceNames", provinceNames);
        sql = sql.replace(":idGame", Long.toString(idGame));
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        results.stream().forEach(input -> pillagedProvinces.add((String) input.get("PROVINCE")));

        return pillagedProvinces;
    }

    /** {@inheritDoc} */
    @Override
    public List<EconomicalSheetEntity> loadSheets(Long idCountry, Integer turn, Long idGame) {
        Criteria criteria = getSession().createCriteria(EconomicalSheetEntity.class);

        criteria.add(Restrictions.eq("turn", turn));
        if (idCountry != null) {
            criteria.add(Restrictions.eq("country.id", idCountry));
        } else {
            Criteria criteriaCountry = criteria.createCriteria("country", "country");
            criteriaCountry.add(Restrictions.eq("game.id", idGame));
        }

        //noinspection unchecked
        return (List<EconomicalSheetEntity>) criteria.list();
    }

    /** {@inheritDoc} */
    @Override
    public Integer getMnuIncome(String name, List<String> pillagedProvinces, Long idGame) {
        String sql = queryProps.getProperty("income.mnu");

        String provinceNames = pillagedProvinces.stream().collect(Collectors.joining("','", "('", "')"));
        sql = sql.replace(":provinceNames", provinceNames);
        sql = sql.replace(":idGame", Long.toString(idGame));
        sql = sql.replace(":countryName", name);

        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    /** {@inheritDoc} */
    @Override
    public Integer getGoldIncome(List<String> provinces, Long idGame) {
        String sql = queryProps.getProperty("income.innergold");

        String provinceNames = provinces.stream().collect(Collectors.joining("','", "('", "')"));
        sql = sql.replace(":provinceNames", provinceNames);

        Integer innerGold = jdbcTemplate.queryForObject(sql, Integer.class);

        sql = queryProps.getProperty("income.outergold");

        sql = sql.replace(":provinceNames", provinceNames);
        sql = sql.replace(":idGame", Long.toString(idGame));

        Integer outerGold = jdbcTemplate.queryForObject(sql, Integer.class);

        return CommonUtil.add(innerGold, outerGold);
    }

    /** {@inheritDoc} */
    @Override
    public Integer getFleetLevelIncome(String name, Long idGame) {
        String sql = queryProps.getProperty("income.tflevels");

        sql = sql.replace(":countryName", name);
        sql = sql.replace(":idGame", Long.toString(idGame));

        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    /** {@inheritDoc} */
    @Override
    public Integer getFleetLevelMonopoly(String name, Long idGame) {
        String sql = queryProps.getProperty("income.tfmonop");

        sql = sql.replace(":countryName", name);
        sql = sql.replace(":idGame", Long.toString(idGame));

        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, List<CounterFaceTypeEnum>> getTradeCenters(Long idGame) {
        Map<String, List<CounterFaceTypeEnum>> tradeCenters = new HashMap<>();

        String sql = queryProps.getProperty("counter.tradeCenter");

        sql = sql.replace(":idGame", Long.toString(idGame));
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        results.stream().forEach(input -> {
            String country = (String) input.get("COUNTRY");
            if (!tradeCenters.containsKey(country)) {
                tradeCenters.put(country, new ArrayList<>());
            }
            tradeCenters.get(country).add(CounterFaceTypeEnum.valueOf((String) input.get("TYPE")));
        });

        return tradeCenters;
    }

    /** {@inheritDoc} */
    @Override
    public Pair<Integer, Integer> getColTpIncome(String name, Long idGame) {
        MutablePair<Integer, Integer> income = new MutablePair<>();

        String sql = queryProps.getProperty("income.colTp");

        sql = sql.replace(":countryName", name);
        sql = sql.replace(":idGame", Long.toString(idGame));
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        results.stream().forEach(input -> {
            String type = (String) input.get("TYPE");
            if (StringUtils.equals("COLONY", type)) {
                income.setLeft(((BigDecimal) input.get("INCOME")).intValue());
            } else {
                income.setRight(((BigDecimal) input.get("INCOME")).intValue());
            }
        });

        return income;
    }

    /** {@inheritDoc} */
    @Override
    public Integer getExoResIncome(String name, Long idGame) {
        String sql = queryProps.getProperty("income.exoRes");

        sql = sql.replace(":countryName", name);
        sql = sql.replace(":idGame", Long.toString(idGame));

        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
