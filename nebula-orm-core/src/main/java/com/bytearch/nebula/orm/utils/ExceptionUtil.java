package com.bytearch.nebula.orm.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionUtil.class);

    static public void printStackTrace(Exception e) {
        logger.error(getExceptionStackTrace(e));
    }

    static public String getExceptionStackTrace(Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        String r = errors.toString();
        try {
            errors.close();
        } catch (IOException ioException) {
            logger.error("getExceptionStackTrace exception: "+ioException.toString());
            ioException.printStackTrace();
        }
        return r;
    }


}
