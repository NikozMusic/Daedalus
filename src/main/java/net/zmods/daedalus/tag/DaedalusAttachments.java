package net.zmods.daedalus.tag;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;

public class DaedalusAttachments {
    public static final AttachmentType<String> ENTITY_DATA = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath("daedalus", "entity_data"),
            builder -> builder.persistent(Codec.STRING).initializer(() -> "{}")
    );
}