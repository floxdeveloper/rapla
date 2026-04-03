package org.rapla.enpoints.server;

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.core.converter.AnnotatedType;

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;

public class ImplClassModelConverter implements ModelConverter {

    private static final Map<Class<?>, Class<?>> INTERFACE_TO_IMPL = Map.ofEntries(
            Map.entry(
                    org.rapla.entities.dynamictype.Classification.class,
                    org.rapla.entities.dynamictype.internal.ClassificationImpl.class),
            Map.entry(
                    org.rapla.entities.domain.Allocatable.class,
                    org.rapla.entities.domain.internal.AllocatableImpl.class)
    );

    @Override
    public Schema resolve(AnnotatedType type,
            ModelConverterContext context,
            Iterator<ModelConverter> chain) {
        System.out.println(type);
        if (type.isSchemaProperty()) {
            JavaType _type = Json.mapper().constructType(type.getType());
            if (_type != null) {
                Class<?> cls = _type.getRawClass();
                Class<?> implClass = INTERFACE_TO_IMPL.get(cls);
                if (implClass != null) {
                    // Nutze die Impl-Klasse statt des Interfaces
                    JavaType implType = Json.mapper().constructType(implClass);
                    AnnotatedType annotatedImplType = new AnnotatedType(implType);
                    System.out.println(implClass);
                    System.out.println(implType);
                    return context.resolve(annotatedImplType);
                }
            }
        }

        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    }
}