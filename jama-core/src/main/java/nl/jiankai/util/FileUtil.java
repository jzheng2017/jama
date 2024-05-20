package nl.jiankai.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class FileUtil {
    private static final String POM_FILE = "pom.xml";

    public static File findPomFile(File projectRootPath) {
        return findFileNonRecursive(projectRootPath, (dir, fileName) -> POM_FILE.equals(fileName));
    }

    public static File findFileNonRecursive(File directory, FilenameFilter filter) {
        File[] foundFiles = directory.listFiles(filter);

        if (foundFiles != null && foundFiles.length > 0) {
            return foundFiles[0];
        }

        throw new FileNotFoundException("Could not find file in directory '%s'".formatted(directory.getName()));
    }

    public static Collection<File> findFileRecursive(File directory, String fileName, String suffix) {
        return FileUtils
                .listFiles(directory, FileFilterUtils.suffixFileFilter(suffix), TrueFileFilter.INSTANCE)
                .stream()
                .filter(file -> fileName.isBlank() || Objects.equals(file.getName(), fileName))
                .toList();
    }

    public static String javaFileNameToFullyQualifiedClass(String javaFileName) {
        int startIndex = javaFileName.indexOf("java/");
        javaFileName = javaFileName.substring(startIndex + 5);
        javaFileName = javaFileName.replace("/", ".");

        return javaFileName.substring(0, javaFileName.length() - 5);
    }

    public static class FileNotFoundException extends RuntimeException {
        public FileNotFoundException(String errorMessage) {
            super(errorMessage);
        }
    }

    public static File findCommonFilePath(Collection<File> descendents) {
        return new File(longestCommonPath(descendents.stream().map(File::getAbsolutePath).toList(), '/'));
    }

    private static String longestCommonPath(final List<String> directories,
                                            final char separator) {
        if (directories.size() == 1) {
            return directories.getFirst().substring(0, directories.getFirst().lastIndexOf("/"));
        }

        int commonCharacters = directories.getFirst().length();
        String commonString = directories.getFirst();

        for (int i = 1; i < directories.size(); i++) {
            String iter = directories.get(i);
            int n = Math.min(commonString.length(), iter.length());
            int j = 0;
            while (j < n && commonString.charAt(j) == iter.charAt(j)) {
                j++;
            }
            if (j < commonCharacters) {
                commonCharacters = j;
            }
            if (iter.compareTo(commonString) > 0) {
                commonString = iter;
            }
        }

        int found = -1;
        for (int i = commonCharacters; i >= 0; --i) {
            if (commonString.charAt(i) == separator) {
                found = i;
                break;
            }
        }

        return commonString.substring(0, found);
    }
}
