package org.rapla.enpoints.server;

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;

public class ImplClassModelConverter implements ModelConverter {

        static {
                // Configure Jackson to use fields instead of getters for serialization
                ObjectMapper mapper = Json.mapper();
                mapper.setVisibility(mapper.getSerializationConfig()
                                .getDefaultVisibilityChecker()
                                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE));
        }

        private static final Map<Class<?>, Class<?>> INTERFACE_TO_IMPL = Map.ofEntries(
                        // org.rapla.entities.dynamictype
                        Map.entry(
                                        org.rapla.entities.dynamictype.Classification.class,
                                        org.rapla.entities.dynamictype.internal.ClassificationImpl.class),
                        Map.entry(
                                        org.rapla.entities.dynamictype.Attribute.class,
                                        org.rapla.entities.dynamictype.internal.AttributeImpl.class),
                        Map.entry(
                                        org.rapla.entities.dynamictype.ClassificationFilter.class,
                                        org.rapla.entities.dynamictype.internal.ClassificationFilterImpl.class),
                        Map.entry(
                                        org.rapla.entities.dynamictype.ClassificationFilterRule.class,
                                        org.rapla.entities.dynamictype.internal.ClassificationFilterRuleImpl.class),
                        Map.entry(
                                        org.rapla.entities.dynamictype.DynamicType.class,
                                        org.rapla.entities.dynamictype.internal.DynamicTypeImpl.class),

                        // org.rapla.entities.domain
                        Map.entry(
                                        org.rapla.entities.domain.Allocatable.class,
                                        org.rapla.entities.domain.internal.AllocatableImpl.class),
                        Map.entry(
                                        org.rapla.entities.domain.Reservation.class,
                                        org.rapla.entities.domain.internal.ReservationImpl.class),
                        Map.entry(
                                        org.rapla.entities.domain.Appointment.class,
                                        org.rapla.entities.domain.internal.AppointmentImpl.class),
                        Map.entry(
                                        org.rapla.entities.domain.Permission.class,
                                        org.rapla.entities.domain.internal.PermissionImpl.class),
                        Map.entry(
                                        org.rapla.entities.domain.Period.class,
                                        org.rapla.entities.domain.internal.PeriodImpl.class),

                        // org.rapla.entities (root level)
                        Map.entry(
                                        org.rapla.entities.Category.class,
                                        org.rapla.entities.internal.CategoryImpl.class),
                        Map.entry(
                                        org.rapla.entities.User.class,
                                        org.rapla.entities.internal.UserImpl.class),

                        // org.rapla.entities.configuration
                        Map.entry(
                                        org.rapla.entities.configuration.CalendarModelConfiguration.class,
                                        org.rapla.entities.configuration.internal.CalendarModelConfigurationImpl.class),
                        Map.entry(
                                        org.rapla.entities.configuration.Preferences.class,
                                        org.rapla.entities.configuration.internal.PreferencesImpl.class),
                        Map.entry(
                                        org.rapla.entities.configuration.RaplaMap.class,
                                        org.rapla.entities.configuration.internal.RaplaMapImpl.class),

                        // org.rapla.entities.storage
                        Map.entry(
                                        org.rapla.entities.storage.ExternalSyncEntity.class,
                                        org.rapla.entities.storage.internal.ExternalSyncEntityImpl.class),

                        // org.rapla.facade
                        Map.entry(
                                        org.rapla.facade.CalendarOptions.class,
                                        org.rapla.facade.internal.CalendarOptionsImpl.class),
                        Map.entry(
                                        org.rapla.facade.CalendarSelectionModel.class,
                                        org.rapla.facade.internal.CalendarModelImpl.class),
                        Map.entry(
                                        org.rapla.facade.Conflict.class,
                                        org.rapla.facade.internal.ConflictImpl.class));

        @Override
        public Schema resolve(AnnotatedType type,
                        ModelConverterContext context,
                        Iterator<ModelConverter> chain) {
                if (type.isSchemaProperty()) {
                        JavaType _type = Json.mapper().constructType(type.getType());
                        if (_type != null) {
                                Class<?> cls = _type.getRawClass();
                                Class<?> implClass = INTERFACE_TO_IMPL.get(cls);
                                if (implClass != null) {
                                        // Nutze die Impl-Klasse statt des Interfaces
                                        JavaType implType = Json.mapper().constructType(implClass);
                                        AnnotatedType annotatedImplType = new AnnotatedType(implType);
                                        return context.resolve(annotatedImplType);
                                }
                        }
                }

                return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        }
}