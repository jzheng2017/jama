package nl.jiankai;

import nl.jiankai.api.Jar;
import nl.jiankai.api.JarDecompiler;
import nl.jiankai.fernflower.FernFlowerDecompiler;

import java.io.File;

public class Jama {
    public static void main(String[] args) {
        JarDecompiler decompiler = new FernFlowerDecompiler();
        decompiler.decompile(new Jar(new File(args[0]), new File(args[1])));
    }
}
