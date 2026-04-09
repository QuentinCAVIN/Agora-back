package com.agora.entity.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

/**
 * Sérialisation JSON explicite : certains drivers (ex. H2) renvoient le JSON comme chaîne,
 * parfois double-encodée — ce convertisseur reste compatible avec une colonne {@code jsonb} PostgreSQL.
 */
@Converter(autoApply = false)
public class AuditDetailsJsonConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Impossible de sérialiser les détails d'audit", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(dbData);
            if (node.isTextual()) {
                return MAPPER.readValue(node.asText(), new TypeReference<>() {});
            }
            if (node.isObject()) {
                return MAPPER.convertValue(node, new TypeReference<>() {});
            }
            return MAPPER.readValue(dbData, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Impossible de désérialiser les détails d'audit", e);
        }
    }
}
