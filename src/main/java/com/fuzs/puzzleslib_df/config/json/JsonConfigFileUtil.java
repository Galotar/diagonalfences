package com.fuzs.puzzleslib_df.config.json;

import com.fuzs.puzzleslib_df.PuzzlesLib;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * handles loading and saving of json config files
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "UnusedReturnValue", "unused"})
public class JsonConfigFileUtil {

    /**
     * gson builder instance, no html escaping to allow "<" and ">"
     */
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    /**
     * how many layers deep recursive file locators are allowed to dig
     */
    private static final int SEARCH_LAYERS = 3;

    /**
     * make a new directory for this mod in the main config directory
     * @param modId directory name
     */
    public static boolean mkdirs(String modId) {

        return mkdirs(getPath(modId));
    }

    /**
     * make a new directory for this mod in the main config directory
     */
    public static boolean mkdirs(File dir) {

        if (dir != null && !dir.exists()) {

            return dir.mkdirs();
        }
        
        return false;
    }

    /**
     * load a json file in the main config directory, create a file if absent
     * @param jsonName name of the file to load
     * @param serializer serializer creates a {@link com.google.gson.JsonElement} and then calls {@link #saveToFile}
     * @param deserializer deserializer feeds a {@link java.io.FileReader} to {@link #GSON} and handles the outcome itself
     */
    public static void getAndLoad(String jsonName, Consumer<File> serializer, Consumer<FileReader> deserializer) {

        File jsonFile = getPath(jsonName);
        load(jsonFile, serializer, deserializer);
    }

    /**
     * load a json file in the mods own config directory inside of the main directory, create a file if absent
     * @param jsonName name of the file to load
     * @param modId config directory name
     * @param serializer serializer creates a {@link com.google.gson.JsonElement} and then calls {@link #saveToFile}
     * @param deserializer deserializer feeds a {@link java.io.FileReader} to {@link #GSON} and handles the outcome itself
     */
    public static void getAndLoad(String jsonName, String modId, Consumer<File> serializer, Consumer<FileReader> deserializer) {

        File jsonFileInDir = getPathInDir(jsonName, modId);
        load(jsonFileInDir, serializer, deserializer);
    }

    /**
     * load a json file in the main config directory, create a file if absent
     * @param jsonName name of the file to load
     * @param serializer serializer creates a {@link com.google.gson.JsonElement} and then calls {@link #saveToFile}
     * @param deserializer deserializer feeds a {@link java.io.FileReader} to {@link #GSON} and handles the outcome itself
     * @param prepareForLoad action to run before loading new files, usually cleaning up a collection
     */
    public static void getAllAndLoad(String jsonName, Consumer<File> serializer, Consumer<FileReader> deserializer, Runnable prepareForLoad) {

        File jsonDir = getPath(jsonName);
        List<File> files = Lists.newArrayList();
        createAllIfAbsent(jsonDir, serializer, files);
        loadAllFiles(jsonDir, deserializer, prepareForLoad, files);
    }

    /**
     * @param jsonDir working directory
     * @param serializer serializer creates a {@link com.google.gson.JsonElement} and then calls {@link #saveToFile}
     * @param files list of found files
     */
    private static void createAllIfAbsent(File jsonDir, Consumer<File> serializer, List<File> files) {

        mkdirs(jsonDir);
        getAllFilesInDir(jsonDir, SEARCH_LAYERS, files, name -> name.endsWith(".json"));
        if (files.isEmpty()) {

            serializer.accept(jsonDir);
        }
    }

    /**
     * @param jsonDir working directory
     * @param deserializer deserializer feeds a {@link java.io.FileReader} to {@link #GSON} and handles the outcome itself
     * @param prepareForLoad action to run before loading new files, usually cleaning up a collection
     * @param files list of found files is empty when files had to be created, already has all required data otherwise and no file search has to be run
     */
    private static void loadAllFiles(File jsonDir, Consumer<FileReader> deserializer, Runnable prepareForLoad, List<File> files) {

        if (files.isEmpty()) {

            getAllFilesInDir(jsonDir, SEARCH_LAYERS, files, name -> name.endsWith(".json"));
        }

        prepareForLoad.run();
        files.forEach(file -> loadFromFile(file, deserializer));
    }

    /**
     * load a json file, create a file if absent
     * @param jsonFile file to read from, or to write to when absent
     * @param serializer serializer creates a {@link com.google.gson.JsonElement} and then calls {@link #saveToFile}
     * @param deserializer deserializer feeds a {@link java.io.FileReader} to {@link #GSON} and handles the outcome itself
     */
    private static void load(File jsonFile, Consumer<File> serializer, Consumer<FileReader> deserializer) {

        createIfAbsent(jsonFile, serializer);
        loadFromFile(jsonFile, deserializer);
    }

    /**
     * create file if absent, creating corresponding directory is handled by serializer
     * @param jsonFile file to create if absent
     * @param serializer serializer creates a {@link com.google.gson.JsonElement} and then calls {@link #saveToFile}
     */
    private static void createIfAbsent(File jsonFile, Consumer<File> serializer) {

        if (!jsonFile.exists()) {

            serializer.accept(jsonFile);
        }
    }

    /**
     * copy <code>jsonName</code> file from classpath to <code>jsonFile</code>
     * @param jsonFile destination file, has to have same name as source
     */
    public static void copyToFile(File jsonFile) {

        mkdirs(jsonFile.getParentFile());
        // has to always be normal slash (even Windows), not File.separator
        try (InputStream input = JsonConfigFileUtil.class.getResourceAsStream("/" + jsonFile.getName());
             FileOutputStream output = new FileOutputStream(jsonFile)) {

            jsonFile.createNewFile();
            byte[] buffer = new byte[16384];
            for (int lengthRead = input.read(buffer); lengthRead > 0; lengthRead = input.read(buffer)) {

                output.write(buffer, 0, lengthRead);
            }
        } catch (Exception e) {

            PuzzlesLib.LOGGER.error("Failed to copy {} in config directory: {}", jsonFile.getName(), e);
        }
    }

    /**
     * save <code>jsonElement</code> to <code>jsonFile</code>
     * @param jsonFile file to save to
     * @param jsonElement {@link com.google.gson.JsonElement} to save
     */
    public static void saveToFile(File jsonFile, JsonElement jsonElement) {

        mkdirs(jsonFile.getParentFile());
        try (FileWriter writer = new FileWriter(jsonFile)) {

            GSON.toJson(jsonElement, writer);
        } catch (Exception e) {

            PuzzlesLib.LOGGER.error("Failed to create {} in config directory: {}", jsonFile.getName(), e);
        }
    }

    /**
     * creates a {@link java.io.FileReader} for <code>file</code> and gives it to a <code>deserializer</code>
     * @param file file to load
     * @param deserializer deserializer feeds a {@link java.io.FileReader} to {@link #GSON} and handles the outcome itself
     */
    private static void loadFromFile(File file, Consumer<FileReader> deserializer) {

        try (FileReader reader = new FileReader(file)) {

            deserializer.accept(reader);
        } catch (Exception e) {

            PuzzlesLib.LOGGER.error("Failed to read {} in config directory: {}", file.getName(), e);
        }
    }

    /**
     * find all files in a directory
     * @param directory directory to search for files
     * @param searchLayers how many recursive layers are allowed
     * @param fileList list to add all found files to
     * @param fileNamePredicate predicate for only finding files of a certain type
     */
    private static void getAllFilesInDir(File directory, int searchLayers, List<File> fileList, Predicate<String> fileNamePredicate) {

        File[] allFilesAndDirs = directory.listFiles();
        if (allFilesAndDirs != null) {

            for (File file : allFilesAndDirs) {

                if (file.isDirectory()) {

                    if (searchLayers > 0) {

                        getAllFilesInDir(file, searchLayers - 1, fileList, fileNamePredicate);
                    }
                } else if (fileList.size() < 128 && fileNamePredicate.test(file.getName())) {

                    try {

                        fileList.add(file);
                    } catch (Exception e) {

                        PuzzlesLib.LOGGER.error("Failed to locate files in {} directory: {}", directory.getName(), e);
                    }
                }
            }
        }
    }

    /**
     * get file in main config directory, can be used as dir or actual file
     * @param jsonName file to get
     * @return file
     */
    public static File getPath(String jsonName) {

        return new File(FMLPaths.CONFIGDIR.get().toFile(), jsonName);
    }

    /**
     * get file in the mod's own config directory
     * @param jsonName file to get
     * @param modId config directory name
     * @return file
     */
    public static File getPathInDir(String jsonName, String modId) {

        return new File(FMLPaths.CONFIGDIR.get().toFile(), modId + File.separator + jsonName);
    }

}
