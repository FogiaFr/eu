package com.mkl.eu.client.service.vo.board;

import com.mkl.eu.client.service.vo.EuObject;

import javax.xml.bind.Unmarshaller;
import java.util.ArrayList;
import java.util.List;

/**
 * Stack of counters (regroupment).
 *
 * @author MKL
 */
public class Stack extends EuObject {
    /** Province where the stack is located. */
    private AbstractProvince province;
    /** Counters of the stack. */
    private List<Counter> counters = new ArrayList<>();

    /** @return the province. */
    public AbstractProvince getProvince() {
        return province;
    }

    /** @param province the province to set. */
    public void setProvince(AbstractProvince province) {
        this.province = province;
    }

    /** @return the counters. */
    public List<Counter> getCounters() {
        return counters;
    }

    /** @param counters the counters to set. */
    public void setCounters(List<Counter> counters) {
        this.counters = counters;
    }

    /**
     * This method is called after all the properties (except IDREF) are unmarshalled for this object,
     * but before this object is set to the parent object.
     *
     * @param unmarshaller the unmarshaller.
     * @param parent       the parent object.
     */
    public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        if (this.province != null) {
            this.province.getStacks().add(this);
        }
        if (this.counters != null) {
            for (Counter counter : counters) {
                counter.setOwner(this);
            }
        }
    }
}
