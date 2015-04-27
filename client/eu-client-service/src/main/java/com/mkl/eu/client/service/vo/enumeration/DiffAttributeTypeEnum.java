package com.mkl.eu.client.service.vo.enumeration;

/**
 * Enumeration for the types of diff attributes.
 *
 * @author MKL.
 */
public enum DiffAttributeTypeEnum {
    /** Where the diff occured (creation/removal of a counter/stack): name of the province. */
    PROVINCE,
    /** Where the diff came from (move of a stack): name of the province. */
    PROVINCE_FROM,
    /** Where the diff went to (move of a stack): name of the province. */
    PROVINCE_TO,
    /** Where the diff came from (move of a counter): id of the stack. */
    STACK_FROM,
    /** Where the diff went to (move of a counter): id of the stack. */
    STACK_TO;
}