package com.ssafy.manager.nutrition.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class FoodApiResponse {

    private Body body;

    @Getter
    @NoArgsConstructor
    public static class Body {
        private Items items;
    }

    @Getter
    @NoArgsConstructor
    public static class Items {
        @JsonDeserialize(using = ItemListDeserializer.class)
        private List<Item> item;
    }

    @Getter
    @NoArgsConstructor
    public static class Item {
        @JsonProperty("FOOD_CD")      private String foodCode;
        @JsonProperty("FOOD_NM_KR")   private String foodName;
        @JsonProperty("SERVING_SIZE") private String servingSize;
        @JsonProperty("AMT_NUM2")     private String calories;
        @JsonProperty("AMT_NUM5")     private String carbs;
        @JsonProperty("AMT_NUM3")     private String protein;
        @JsonProperty("AMT_NUM4")     private String fat;
        @JsonProperty("AMT_NUM7")     private String fiber;
    }

    static class ItemListDeserializer extends StdDeserializer<List<Item>> {

        ItemListDeserializer() {
            super(List.class);
        }

        @Override
        public List<Item> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode node = mapper.readTree(p);
            if (node.isArray()) {
                List<Item> result = new ArrayList<>();
                for (JsonNode element : node) {
                    result.add(mapper.treeToValue(element, Item.class));
                }
                return result;
            }
            return List.of(mapper.treeToValue(node, Item.class));
        }
    }
}
