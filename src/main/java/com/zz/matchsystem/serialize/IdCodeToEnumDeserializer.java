package com.zz.matchsystem.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.zz.matchsystem.model.BaseEnum;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

/**
*@Author：EvilSay
*@Date：18.10.23  00:21
*@description:
*/
public class IdCodeToEnumDeserializer extends JsonDeserializer<BaseEnum> {
    @Override
    public BaseEnum deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        final String param = jsonParser.getText();
        final JsonStreamContext parsingContext = jsonParser.getParsingContext();
        final String currentName = parsingContext.getCurrentName();
        final Object currentValue = parsingContext.getCurrentValue();
        try {
            final Field declaredField = currentValue.getClass().getDeclaredField(currentName);
            final Class<?> targetType = declaredField.getType();
            final Method createMethod = targetType.getDeclaredMethod("create", Object.class);
            return (BaseEnum) createMethod.invoke(null, param);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException e) {
            throw new RemoteException("");
        }
    }
}

