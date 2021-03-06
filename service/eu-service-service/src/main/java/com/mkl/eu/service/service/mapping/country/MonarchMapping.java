package com.mkl.eu.service.service.mapping.country;

import com.mkl.eu.client.service.vo.country.Monarch;
import com.mkl.eu.client.service.vo.country.PlayableCountry;
import com.mkl.eu.service.service.mapping.AbstractMapping;
import com.mkl.eu.service.service.persistence.oe.country.MonarchEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mapping between VO and OE for a Monarch.
 *
 * @author MKL.
 */
@Component
public class MonarchMapping extends AbstractMapping {
    /** Mapping for a country. */
    @Autowired
    private PlayableCountryMapping playableCountryMapping;

    /**
     * OEs to VOs.
     *
     * @param sources        object source.
     * @param objectsCreated Objects created by the mappings (sort of caching).
     * @return object mapped.
     */
    public List<Monarch> oesToVos(List<MonarchEntity> sources, final Map<Class<?>, Map<Long, Object>> objectsCreated) {
        if (sources == null) {
            return null;
        }

        List<Monarch> targets = new ArrayList<>();

        for (MonarchEntity source : sources) {
            Monarch target = storeVo(Monarch.class, source, objectsCreated, this::oeToVo);
            if (target != null) {
                targets.add(target);
            }
        }

        return targets;
    }

    /**
     * OE to VO.
     *
     * @param source         object source.
     * @param objectsCreated Objects created by the mappings (sort of caching).
     * @return object mapped.
     */
    public Monarch oeToVo(MonarchEntity source, final Map<Class<?>, Map<Long, Object>> objectsCreated) {
        if (source == null) {
            return null;
        }

        Monarch target = new Monarch();

        target.setId(source.getId());
        target.setBegin(source.getBegin());
        target.setEnd(source.getEnd());
        target.setAdministrative(source.getAdministrative());
        target.setDiplomacy(source.getDiplomacy());
        target.setMilitary(source.getMilitary());
        target.setMilitaryAverage(source.getMilitaryAverage());
        target.setCountry(storeVo(PlayableCountry.class, source.getCountry(), objectsCreated, playableCountryMapping::oeToVo));

        return target;
    }
}
