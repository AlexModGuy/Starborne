package net.starborne.server.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StarborneTransformer implements IClassTransformer {
    private List<Transformer> transformers = new ArrayList<>();

    public StarborneTransformer() {
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes != null) {
            List<Transformer> apply = new ArrayList<>();
            for (Transformer transformer : this.transformers) {
                if (transformer.applies(transformedName)) {
                    apply.add(transformer);
                }
            }
            if (apply.size() > 0) {
                ClassReader classReader = new ClassReader(bytes);
                ClassNode classNode = new ClassNode();
                classReader.accept(classNode, 0);
                boolean transformed = false;
                for (Transformer transformer : apply) {
                    transformed |= transformer.transform(classNode, name.equals(transformedName), transformedName);
                }
                if (transformed) {
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                    classNode.accept(classWriter);
                    this.saveBytecode(transformedName, classWriter);
                    bytes = classWriter.toByteArray();
                }
            }
        }
        return bytes;
    }

    private void saveBytecode(String name, ClassWriter classWriter) {
        try {
            File debugDir = new File("llibrary/debug/starborne/");
            if (debugDir.exists()) {
                debugDir.delete();
            }
            debugDir.mkdirs();
            FileOutputStream out = new FileOutputStream(new File(debugDir, name + ".class"));
            out.write(classWriter.toByteArray());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
