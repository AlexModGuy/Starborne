package net.starborne.server.core;

import org.objectweb.asm.tree.ClassNode;

public interface Transformer {
    boolean applies(String transformedName);

    boolean transform(ClassNode classNode, boolean development, String transformedName);
}
