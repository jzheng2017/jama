package nl.jiankai.fernflower;

import nl.jiankai.api.DecompiledJar;
import nl.jiankai.api.Jar;
import nl.jiankai.api.JarDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;

public class FernFlowerDecompiler implements JarDecompiler {
    @Override
    public DecompiledJar decompile(Jar jar) {
        ConsoleDecompiler.main(new String[]{jar.sourceJarPath().getAbsolutePath(), jar.decompiledJarDestination().getAbsolutePath()});
        return new DecompiledJar(jar.decompiledJarDestination());
    }
}
