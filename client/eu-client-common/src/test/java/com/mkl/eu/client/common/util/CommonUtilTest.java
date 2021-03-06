package com.mkl.eu.client.common.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mkl.eu.client.common.util.CommonUtil.EPSILON;

/**
 * Unit test for CommonUtil.
 *
 * @author MKL
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class CommonUtilTest {
    @Test
    public void testFindFirst() {
        List<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");
        list.add("three");

        String find = CommonUtil.findFirst(list, s -> StringUtils.equals(s, "one"));

        Assert.assertEquals("one", find);

        find = CommonUtil.findFirst(list, s -> StringUtils.equals(s, "four"));

        Assert.assertEquals(null, find);
    }

    @Test
    public void testNumbers() {
        Assert.assertEquals(null, CommonUtil.add((Integer)null));
        Assert.assertEquals(null, CommonUtil.add((Integer)null, null));
        Assert.assertEquals(1, CommonUtil.add(1, null).intValue());
        Assert.assertEquals(2, CommonUtil.add(null, 2).intValue());
        Assert.assertEquals(3, CommonUtil.add(1, 2).intValue());
        Assert.assertEquals(10, CommonUtil.add(1, 2, 3, 4).intValue());

        Double delta = 0.00001;

        Assert.assertEquals(null, CommonUtil.add((Double)null));
        Assert.assertEquals(null, CommonUtil.add((Double)null, null));
        Assert.assertEquals(1d, CommonUtil.add(1d, null).doubleValue(), delta);
        Assert.assertEquals(2d, CommonUtil.add(null, 2d).doubleValue(), delta);
        Assert.assertEquals(3d, CommonUtil.add(1d, 2d).doubleValue(), delta);
        Assert.assertEquals(10d, CommonUtil.add(1d, 2d, 3d, 4d).doubleValue(), delta);
        Assert.assertEquals(11.7d, CommonUtil.add(1.5d, 2.7d, 3.2d, 4.3d).doubleValue(), delta);

        Assert.assertEquals(0d, CommonUtil.min(null, null), delta);
        Assert.assertEquals(0d, CommonUtil.min(null, 12d), delta);
        Assert.assertEquals(0d, CommonUtil.min(12d, null), delta);
        Assert.assertEquals(-12d, CommonUtil.min(null, -12d), delta);
        Assert.assertEquals(-12d, CommonUtil.min(-12d, null), delta);
        Assert.assertEquals(-13d, CommonUtil.min(-12d, -13d), delta);
        Assert.assertEquals(-12d, CommonUtil.min(-12d, 13d), delta);
        Assert.assertEquals(12d, CommonUtil.min(12d, 13d), delta);
    }

    @Test
    public void testAddSubtractOne() {
        Map<String, Integer> map = new HashMap<>();

        CommonUtil.addOne(null, null);
        CommonUtil.addOne(null, "one");

        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);

        CommonUtil.addOne(map, "one");

        Assert.assertEquals(4, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(2, map.get("two").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(4, map.get("four").intValue());

        CommonUtil.addOne(map, "four");

        Assert.assertEquals(4, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(2, map.get("two").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(5, map.get("four").intValue());

        CommonUtil.addOne(map, "five");

        Assert.assertEquals(5, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(2, map.get("two").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(5, map.get("four").intValue());
        Assert.assertEquals(1, map.get("five").intValue());

        CommonUtil.addOne(map, null);

        Assert.assertEquals(6, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(2, map.get("two").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(5, map.get("four").intValue());
        Assert.assertEquals(1, map.get("five").intValue());
        Assert.assertEquals(1, map.get(null).intValue());

        CommonUtil.subtractOne(null, null);
        CommonUtil.subtractOne(null, "one");
        CommonUtil.subtractOne(map, null);

        Assert.assertEquals(5, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(2, map.get("two").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(5, map.get("four").intValue());
        Assert.assertEquals(1, map.get("five").intValue());

        CommonUtil.subtractOne(map, "two");

        Assert.assertEquals(5, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(1, map.get("two").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(5, map.get("four").intValue());
        Assert.assertEquals(1, map.get("five").intValue());

        CommonUtil.subtractOne(map, "two");

        Assert.assertEquals(4, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(5, map.get("four").intValue());
        Assert.assertEquals(1, map.get("five").intValue());

        map = new HashMap<>();
        CommonUtil.add(map, "one", 5);

        Assert.assertEquals(1, map.size());
        Assert.assertEquals(5, map.get("one").intValue());

        CommonUtil.add(map, "one", 3);

        Assert.assertEquals(1, map.size());
        Assert.assertEquals(8, map.get("one").intValue());

        CommonUtil.add(map, "two", 2);

        Assert.assertEquals(2, map.size());
        Assert.assertEquals(8, map.get("one").intValue());
        Assert.assertEquals(2, map.get("two").intValue());
    }

    @Test
    public void testAddSubtractOneLong() {
        Map<String, Long> map = new HashMap<>();

        CommonUtil.addOneLong(null, null);
        CommonUtil.addOneLong(null, "one");

        map.put("one", 1l);
        map.put("two", 2l);
        map.put("three", 3l);
        map.put("four", 4l);

        CommonUtil.addOneLong(map, "one");

        Assert.assertEquals(4, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(2, map.get("two").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(4, map.get("four").intValue());

        CommonUtil.addOneLong(map, "four");

        Assert.assertEquals(4, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(2, map.get("two").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(5, map.get("four").intValue());

        CommonUtil.addOneLong(map, "five");

        Assert.assertEquals(5, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(2, map.get("two").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(5, map.get("four").intValue());
        Assert.assertEquals(1, map.get("five").intValue());

        CommonUtil.addOneLong(map, null);

        Assert.assertEquals(6, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(2, map.get("two").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(5, map.get("four").intValue());
        Assert.assertEquals(1, map.get("five").intValue());
        Assert.assertEquals(1, map.get(null).intValue());

        CommonUtil.subtractOneLong(null, null);
        CommonUtil.subtractOneLong(null, "one");
        CommonUtil.subtractOneLong(map, null);

        Assert.assertEquals(5, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(2, map.get("two").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(5, map.get("four").intValue());
        Assert.assertEquals(1, map.get("five").intValue());

        CommonUtil.subtractOneLong(map, "two");

        Assert.assertEquals(5, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(1, map.get("two").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(5, map.get("four").intValue());
        Assert.assertEquals(1, map.get("five").intValue());

        CommonUtil.subtractOneLong(map, "two");

        Assert.assertEquals(4, map.size());
        Assert.assertEquals(2, map.get("one").intValue());
        Assert.assertEquals(3, map.get("three").intValue());
        Assert.assertEquals(5, map.get("four").intValue());
        Assert.assertEquals(1, map.get("five").intValue());

        map = new HashMap<>();
        CommonUtil.addLong(map, "one", 5l);

        Assert.assertEquals(1, map.size());
        Assert.assertEquals(5, map.get("one").intValue());

        CommonUtil.addLong(map, "one", 3l);

        Assert.assertEquals(1, map.size());
        Assert.assertEquals(8, map.get("one").intValue());

        CommonUtil.addLong(map, "two", 2l);

        Assert.assertEquals(2, map.size());
        Assert.assertEquals(8, map.get("one").intValue());
        Assert.assertEquals(2, map.get("two").intValue());
    }

    @Test
    public void testAddSubtractOneDouble() {
        Map<String, Double> map = new HashMap<>();

        CommonUtil.add(map, "one", 5d);

        Assert.assertEquals(1, map.size());
        Assert.assertEquals(5d, map.get("one"), EPSILON);

        CommonUtil.add(map, "one", 3d);

        Assert.assertEquals(1, map.size());
        Assert.assertEquals(8d, map.get("one"), EPSILON);

        CommonUtil.add(map, "two", 2d);

        Assert.assertEquals(2, map.size());
        Assert.assertEquals(8d, map.get("one"), EPSILON);
        Assert.assertEquals(2d, map.get("two"), EPSILON);
    }

    @Test
    public void testIntegers() {
        Assert.assertEquals(0, CommonUtil.toInt((Integer) null));
        Assert.assertEquals(1, CommonUtil.toInt(new Integer("1")));
        Assert.assertEquals(1, CommonUtil.toInt(1));

        Assert.assertEquals(0, CommonUtil.toInt((String) null));
        Assert.assertEquals(0, CommonUtil.toInt(""));
        Assert.assertEquals(0, CommonUtil.toInt("abc"));
        Assert.assertEquals(0, CommonUtil.toInt("1a2"));
        Assert.assertEquals(1, CommonUtil.toInt("1"));
        Assert.assertEquals(-5, CommonUtil.toInt("-5"));
        Assert.assertEquals(12, CommonUtil.toInt("12"));

        Assert.assertEquals(0, CommonUtil.subtract(null));
        Assert.assertEquals(0, CommonUtil.subtract(null, 3));
        Assert.assertEquals(0, CommonUtil.subtract(null, 3, null));
        Assert.assertEquals(0, CommonUtil.subtract(null, 3, null, 4));
        Assert.assertEquals(2, CommonUtil.subtract(5, 3));
        Assert.assertEquals(2, CommonUtil.subtract(5, 3, null));
        Assert.assertEquals(0, CommonUtil.subtract(null, 3, null, 4));
    }

    @Test
    public void testEquals() {
        Assert.assertTrue(CommonUtil.equals(null, null));
        Assert.assertTrue(CommonUtil.equals(null, 0));
        Assert.assertTrue(CommonUtil.equals(0, null));
        Assert.assertTrue(CommonUtil.equals(0, 0));
        Assert.assertTrue(CommonUtil.equals(1, 1));

        Assert.assertFalse(CommonUtil.equals(1, 0));
        Assert.assertFalse(CommonUtil.equals(0, 1));
        Assert.assertFalse(CommonUtil.equals(1, null));
        Assert.assertFalse(CommonUtil.equals(null, 1));
    }
}
