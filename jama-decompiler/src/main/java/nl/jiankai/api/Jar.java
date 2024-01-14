package nl.jiankai.api;

import java.io.File;

public record Jar(File sourceJarPath, File decompiledJarDestination) {
}
