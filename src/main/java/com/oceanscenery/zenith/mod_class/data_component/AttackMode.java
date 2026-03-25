package com.oceanscenery.zenith.mod_class.data_component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Objects;

public record AttackMode(Mode mode,boolean attackPlayer) {

    public static final HashMap<String,Mode> MODE_HASH_MAP=new HashMap<>();
    static{
        MODE_HASH_MAP.put("living_entity",Mode.LIVING_ENTITY);
        MODE_HASH_MAP.put("attackable_entity",Mode.ATTACKABLE_ENTITY);
        MODE_HASH_MAP.put("all",Mode.ALL);
    }

    public enum Mode{
        LIVING_ENTITY("living_entity"),
        ATTACKABLE_ENTITY("attackable_entity"),
        ALL("all");

        public final String entityMode;

        Mode(String entityMode) {
            this.entityMode=entityMode;
        }
    }

    public AttackMode(String atk,boolean attackPlayer){
        this(MODE_HASH_MAP.get(atk),attackPlayer);
    }

    public Mode getMode(){
        return this.mode;
    }

    public String getStrMode(){
        return this.mode.entityMode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AttackMode(Mode mode1, boolean attackPlayer1))) return false;
        return mode == mode1 && attackPlayer==attackPlayer1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, attackPlayer);
    }

    public static final Codec<AttackMode> CODEC=RecordCodecBuilder.create(
            attackModeInstance -> attackModeInstance.group(
                    Codec.STRING.fieldOf("attack_mode").forGetter(AttackMode::getStrMode),
                    Codec.BOOL.fieldOf("attack_player").forGetter(AttackMode::attackPlayer)
            ).apply(attackModeInstance,(str,boo)-> MODE_HASH_MAP.containsKey(str)?new AttackMode(str,boo):new AttackMode(Mode.LIVING_ENTITY,true))
    );
}
