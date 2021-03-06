/*
 * Copyright (C) 2017 XStream Committers.
 * All rights reserved.
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 * Created on 13. January 2017 by Matej Cimbora
 */
package com.thoughtworks.xstream.converters.time;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.SingleValueConverter;


/**
 * Converts a {@link LocalTime} to a string.
 *
 * @author Matej Cimbora
 * @since 1.4.10
 */
public class LocalTimeConverter implements SingleValueConverter {

    private static final DateTimeFormatter FORMATTER;

    static {
        FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .toFormatter();
    }

    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") final Class type) {
        return LocalTime.class == type;
    }

    @Override
    public Object fromString(final String str) {
        try {
            return LocalTime.parse(str);
        } catch (final DateTimeParseException e) {
            final ConversionException exception = new ConversionException("Cannot parse value as local time", e);
            exception.add("value", str);
            throw exception;
        }
    }

    @Override
    public String toString(final Object obj) {
        if (obj == null) {
            return null;
        }

        final LocalTime localTime = (LocalTime)obj;
        return FORMATTER.format(localTime);
    }
}
