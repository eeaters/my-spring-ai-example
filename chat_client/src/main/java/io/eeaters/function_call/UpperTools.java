package io.eeaters.function_call;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yjwan
 * @version 1.0
 */
public class UpperTools {

    private static final Logger log = LoggerFactory.getLogger(UpperTools.class);

    public static String upperTools(String input) {
        log.info(input);
        return input.toUpperCase();
    }

}

    