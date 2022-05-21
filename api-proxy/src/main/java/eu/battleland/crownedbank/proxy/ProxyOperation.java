package eu.battleland.crownedbank.proxy;

import java.util.function.Function;

/**
 * Operation
 */
public enum ProxyOperation {
    PROVIDE_REQUEST,
    PROVIDE_RESPONSE,

    WITHDRAW_REQUEST,
    WITHDRAW_RESPONSE,

    DEPOSIT_REQUEST,
    DEPOSIT_RESPONSE;

}
