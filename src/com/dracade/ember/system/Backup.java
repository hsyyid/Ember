package com.dracade.ember.system;

import com.dracade.ember.Ember;
import com.dracade.ember.exceptions.IllegalBackupDestination;
import com.google.common.base.Optional;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.api.world.World;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Ember's backup manager.
 * Used for loading and storing world saves.
 */
public final class Backup {

    // Backup singleton
    private static Backup instance;

    /**
     * @param path The backup directory
     * @return Backup singleton instance
     */
    public static Backup instance(String path) {
        return (Backup.instance == null) ? (Backup.instance = new Backup(path)) : instance;
    }

    // Directory for the backups
    private File backupDirectory;

    // Directory where the worlds are located
    private String worldsDirectory;

    /**
     * Constructs Ember's BackupManager
     *
     * @param path The destination folder for the backups
     */
    private Backup(String path) {
        // Set the destination folder.
        backupDirectory = new File(path);

        // If the backup directory does not exist
        if (!backupDirectory.exists()) {
            backupDirectory.mkdir();
        }

        // Throw a runtime exception if the directory doesn't exist.
        // Throw a runtime exception if the destination isn't a directory.
        if (!backupDirectory.exists() || !backupDirectory.isDirectory()) {
            throw new IllegalBackupDestination("The backup destination is either not a directory or it could not be created.");
        }

        try {
            // A world is named after it's directory, so it's safe to assume
            // that there must be a directory with the same name as the world.

            // Build a loader and create a ConfiguratioNode from that, then get the 'level-name' from the server.properties
            HoconConfigurationLoader loader = HoconConfigurationLoader.builder().setFile(new File("server.properties")).build();
            ConfigurationNode node = loader.load();

            // Create the serverDir File from that.
            worldsDirectory = (String) node.getNode("level-name").getValue();

        } catch ( IOException e ) {
            // Throw a runtime exception if we can't load the server.properties file.
            throw new RuntimeException("Unable to load server.properties!\n" + e.getMessage());
        }
    }

    /**
     * Creates a backup of the chosen world.
     * Make sure the chosen world is unloaded!
     *
     * @Param worldName The world's name to backup.
     */
    public void backupWorld(String worldName) {
        // Try to get the world to backup
        Optional<World> worldOptional = Ember.game().getServer().getWorld(worldName);

        // If the world does not exist throw an exception
        if (!worldOptional.isPresent()) {
            throw new RuntimeException(String.format("Cannot backup %s because the world does not exist!", worldName));
        }

        // Archive the world.
        this.createCompressedBackup(worldName, "Worlds");
    }

    /**
     * Creates a compressed backup from the specified source directory and writes it to
     * the specified destinationFolder that is prefixed by the default backupDirectory
     *
     * @param source The directory to create a backup from
     * @param destinationFolder The directory to write it to
     */
    private void createCompressedBackup(String source, String destinationFolder) {
        // Prefix the world with the world directory.
        File sourceFile = new File( worldsDirectory + File.separator + source );

        // New destination directory: e.g. "backup/worlds/..."
        File destinationPath = new File(backupDirectory.getAbsolutePath().concat(File.separator + destinationFolder));

        // If the destination directory does not exist
        // then create a new one.
        if (!destinationPath.exists()) {
            destinationPath.mkdir();
        }

        // Throw a runtime exception if the directory doesn't exist.
        // Throw a runtime exception if the destination isn't a directory.
        if (!backupDirectory.exists() || !backupDirectory.isDirectory()) {
            throw new IllegalBackupDestination("Backup destination is not a folder or does not exist! " + backupDirectory.getAbsolutePath());
        }

        // Throw a runtime exception if the source file does not exist.
        if (!sourceFile.exists()) {
            throw new IllegalBackupDestination("Backup source does not exist! " + sourceFile.getAbsolutePath());
        }

        // Catch any exception that might happen.
        try {
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destinationPath.getPath().concat(File.separator + sourceFile.getName() + ".zip")));

            // Variables for the reading and writing to the zipentry
            byte[] buffer = new byte[1024];
            int len = 0;

            String[] filesToCopy = generateFileList(sourceFile);

            // Iterate through all the files and copy them
            for (String f : filesToCopy) {

                // Create a new zipEntry and add it to the outputStream but remove the worldname beginning in the zip file
                // so we don't end up with a zip called "World.zip" that contains a directory called "World"
                ZipEntry ze = new ZipEntry(f.substring(sourceFile.getName().length()+1));
                zipOutputStream.putNextEntry(ze);

                // Open the file to be written to the zip
                FileInputStream fileInputStream = new FileInputStream(worldsDirectory + File.separator + f);

                // Loop and write the data we have available
                while ((len = fileInputStream.read(buffer, 0, buffer.length)) > 0 ){
                    zipOutputStream.write(buffer, 0, len);
                }

                // Close the stream
                fileInputStream.close();
                zipOutputStream.closeEntry();
            }

            // Close the zip file.
            zipOutputStream.close();
        } catch(IOException e) {
            // Print the exception to the console.
            e.printStackTrace();
        }
    }

    /**
     * Returns a full list of files prefixed by their directories relative to
     * the starting directory.
     *
     * @param source The starting directory
     * @return A list of files prefixed with their relative directories
     */
    private String[] generateFileList(File source) {
        // Create a list to store the entries temporarily
        List<String> files = new ArrayList<String>();

        // Iterate through the current directory to add files.
        for (File file : source.listFiles()) {
            // If the file is a directory then call this function again
            if (file.isDirectory()) {
                // Loop through the received files and add them
                Collections.addAll(files, generateFileList(file));
            }else {
                // Get the world's path
                String path = file.getPath();

                // Remove the worlds directory from the pathname,
                // so that the zip entry does not prefix everything with the world directory.
                files.add(path.substring(worldsDirectory.length() + 1, path.length()));
            }
        }

        // return an array.
        return files.toArray(new String[files.size()]);
    }

    /**
     * Loads a world from the backup folder and copies it in the worlds folder under the same name.
     *
     * @param backupName The backup worldname
     */
    public void loadWorld(String backupName) {
        this.loadWorld(backupName, backupName, false);
    }

    /**
     * Loads a world from the backup folder and copies it in the worlds folder under a different name.
     *
     * @param backupName The world name in the backups
     * @param worldName The destination world name
     */
    public void loadWorld(String backupName, String worldName) {
        this.loadWorld(backupName, worldName, false);
    }

    /**
     * Loads a world from the backup folder and copies it in the worlds folder.
     *
     * @param backupName The backup world name
     * @param worldName The destination world name
     * @param overwrite Overwrite destination if it already exists?
     */
    public void loadWorld(String backupName, String worldName, boolean overwrite) {
        // If the world already exists...
        if (Ember.game().getServer().getWorld(worldName).isPresent()) {
            // If we dont wan't to overwrite the existing file, throw an exception.
            if (!overwrite) {
                throw new RuntimeException("Unable to load the file from backup. The world already exists.");
            } else {
                // Remove the existing world.
                File existingWorld = new File(worldsDirectory + File.separator + worldName);

                // Make sure that the folder we're about to remove contains the two essential world files
                // so that we don't accidentally remove the wrong folder.
                if (Arrays.asList(existingWorld.listFiles()).contains("level.dat") && Arrays.asList(existingWorld.listFiles()).contains("level_sponge.dat")) {
                    existingWorld.delete();
                }
            }
        }

        File worldDir = new File(worldsDirectory + File.separator + worldName);

        if (!worldDir.exists()) {
            worldDir.mkdir();
        }

        try {
            // Try to load the zipfile.
            ZipFile zipFile = new ZipFile(backupDirectory + File.separator + "worlds" + File.separator + backupName + ".zip");

            // Get all the entries in the zip file
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            // Variables for reading and writing
            byte[] buffer = new byte[1024];
            int len = 0;

            // While there are more entries.
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();

                // Get the entry as a file.
                File entryFile = new File(worldsDirectory + File.separator + worldName + File.separator + ze.getName());

                if (ze.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    entryFile.getParentFile().mkdirs();
                }

                // Create the output stream for the new file and the input stream from the zipfile
                FileOutputStream fileOutputStream = new FileOutputStream(entryFile);
                InputStream entryInputStream = zipFile.getInputStream(ze);

                // While there is something to read then write it to the output file
                while ((len = entryInputStream.read(buffer, 0, buffer.length)) > 0) {
                    fileOutputStream.write(buffer , 0, buffer.length);
                }

                // Close all the streams.
                entryInputStream.close();
                fileOutputStream.close();
            }

            // Close the zip file.
            zipFile.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
