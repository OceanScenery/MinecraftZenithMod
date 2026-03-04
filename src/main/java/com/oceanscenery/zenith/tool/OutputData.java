package com.oceanscenery.zenith.tool;

import com.oceanscenery.zenith.client.ZenithProjectileRenderer;

public class OutputData {
    public static void main(String[] args) {
        for(int i=1;i<21;i++){
            System.out.println(".override()\n" +
                    "                .predicate(ResourceLocation.parse(\"custom_model_data\"), "+i+")\n" +
                    "                .model(new ModelFile.UncheckedModelFile(modLoc(\"item/"+ ZenithProjectileRenderer.SWORD_MODEL[i]+"\")))\n" +
                    "                .end()\n");
        }
    }
}
