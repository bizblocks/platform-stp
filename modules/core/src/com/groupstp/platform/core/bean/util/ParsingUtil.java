package com.groupstp.platform.core.bean.util;


import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.UuidProvider;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Helper class which provide basic parsing functionality
 *
 * @author adiatullin
 */
@Component(ParsingUtil.NAME)
public final class ParsingUtil {
    public static final String NAME = "plstp_ParsingUtil";

    public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    @Inject
    private Messages messages;

    /**
     * Parse and return value from text to expected representation java class.
     * Note: collection and complex entities are not supported.
     *
     * @param text  data in the text.
     * @param clazz expected Java type class.
     * @param <T>   The Java type to return.
     * @return value in expected java type.
     */
    @Nullable
    public <T> T toSimpleValue(@Nullable String text, Class clazz) {
        return toSimpleValue(text, clazz, DEFAULT_DATE_FORMAT);
    }

    /**
     * Transforming provided value to it's text representation.
     *
     * @param value object which will be converted to text.
     * @return converted object text.
     */
    public String toText(@Nullable Object value) {
        return toText(value, DEFAULT_DATE_FORMAT);
    }

    /**
     * Parse and return value from text to expected representation java class.
     * Note: collection and complex entities are not supported.
     *
     * @param text       data in the text.
     * @param clazz      expected Java type class.
     * @param dateFormat specific date format for parsing if expected value can be date.
     * @param <T>        The Java type to return.
     * @return value in expected java type.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T toSimpleValue(@Nullable String text, Class clazz, SimpleDateFormat dateFormat) {
        if (!StringUtils.isEmpty(text)) {
            try {
                if (String.class.equals(clazz)) {
                    return (T) text;
                } else if (Integer.class.equals(clazz)) {
                    text = prepareNumberText(text);
                    try {
                        return (T) Integer.valueOf(text);
                    } catch (Exception ignore) {
                        return (T) (Integer) Double.valueOf(text).intValue();
                    }
                } else if (Long.class.equals(clazz)) {
                    text = prepareNumberText(text);
                    try {
                        return (T) Long.valueOf(text);
                    } catch (Exception ignore) {
                        return (T) (Long) Double.valueOf(text).longValue();
                    }
                } else if (Double.class.equals(clazz)) {
                    return (T) Double.valueOf(prepareNumberText(text));
                } else if (Float.class.equals(clazz)) {
                    return (T) Float.valueOf(prepareNumberText(text));
                } else if (BigDecimal.class.equals(clazz)) {
                    return (T) new BigDecimal(prepareNumberText(text));
                } else if (Boolean.class.equals(clazz)) {
                    Boolean value = textToBooleanByLocale(text);
                    if (value != null) {
                        return (T) value;
                    }
                    return (T) BooleanUtils.toBooleanObject(text);
                } else if (Date.class.equals(clazz)) {
                    return (T) dateFormat.parse(text);
                } else if (Enum.class.isAssignableFrom(clazz)) {
                    return (T) Enum.valueOf(clazz, text);
                } else if (UUID.class.equals(clazz)) {
                    return (T) UuidProvider.fromString(text);
                }
            } catch (Exception e) {
                throw new RuntimeException(String.format("Failed to parse value '%s' for class '%s'", text, clazz.getSimpleName()), e);
            }
            throw new RuntimeException(String.format("Unknown value '%s' for class '%s'", text, clazz.getSimpleName()));
        }
        return null;
    }

    private String prepareNumberText(String text) {
        return text.replace(",", ".").replaceAll("\\s", StringUtils.EMPTY);
    }

    /**
     * Transforming provided value to it's text representation.
     *
     * @param value      object which will be converted to text.
     * @param dateFormat specific date format for date object
     * @return converted object text.
     */
    public String toText(@Nullable Object value, SimpleDateFormat dateFormat) {
        if (value == null) {
            return StringUtils.EMPTY;
        }
        Class clazz = value.getClass();
        if (Date.class.equals(clazz)) {
            return dateFormat.format((Date) value);
        } else if (Boolean.class.equals(clazz)) {
            return booleanToTextByLocale((Boolean) value);
        }
        return String.valueOf(value);
    }

    @Nullable
    protected Boolean textToBooleanByLocale(String text) {
        if (!StringUtils.isEmpty(text)) {
            text = text.trim();
            if (text.equalsIgnoreCase(messages.getMainMessage("trueString"))) {
                return Boolean.TRUE;
            }
            if (text.equalsIgnoreCase(messages.getMainMessage("falseString"))) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    @Nullable
    protected String booleanToTextByLocale(Boolean value) {
        if (Boolean.TRUE.equals(value)) {
            return messages.getMainMessage("trueString");
        }
        if (Boolean.FALSE.equals(value)) {
            return messages.getMainMessage("falseString");
        }
        return null;
    }
}