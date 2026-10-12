package com.oceanscenery.zenith.class_bytecode;

import com.oceanscenery.zenith.class_bytecode.common.LivingEntityHealthPatcher;
import com.oceanscenery.zenith.class_bytecode.common.LivingEntityHurtPatcher;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;

import java.util.Set;

public final class LivingEntityProcessor implements ClassProcessor {
    private LivingEntityHealthPatcher healthPatcher;
    private LivingEntityHurtPatcher hurtPatcher;

    @Override
    public ProcessorName name() {
        return ProcessorName.parse("zzz_zenith:living_entity");
    }

    @Override
    public void link(LinkContext context) {
        ClassProcessor.super.link(context);
        var bytecodeProvider = context.bytecodeProvider();
        this.healthPatcher = new LivingEntityHealthPatcher(
            bytecodeProvider::getByteCode
        );
        this.hurtPatcher = new LivingEntityHurtPatcher(
            bytecodeProvider::getByteCode
        );
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return !context.empty();
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        if (healthPatcher == null) {
            throw new IllegalStateException("LivingEntityProcessor has not been linked");
        }


        boolean flag = hurtPatcher.transform(context.node());
        flag |= healthPatcher.transform(context.node());

        return flag
            ? ComputeFlags.COMPUTE_FRAMES
            : ComputeFlags.NO_REWRITE;
    }

    @Override
    public Set<ProcessorName> runsAfter() {
        return Set.of(
            ClassProcessorIds.MIXIN,
            ClassProcessorIds.COMPUTING_FRAMES
        );
    }

    @Override
    public OrderingHint orderingHint() {
        return OrderingHint.LATE;
    }
}
