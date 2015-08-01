/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Dracade
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.dracade.ember;

import com.dracade.ember.Exceptions.IllegalBackupDestination;
import com.dracade.ember.core.Arena;
import com.dracade.ember.core.Minigame;
import com.dracade.ember.core.adapters.ClassAdapter;
import com.dracade.ember.core.adapters.WorldAdapter;
import com.dracade.ember.core.events.minigame.MinigameStartedEvent;
import com.dracade.ember.core.events.minigame.MinigameStoppedEvent;
import com.dracade.ember.core.events.minigame.MinigameStoppingEvent;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.state.InitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.scheduler.Task;
import org.spongepowered.api.world.World;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Plugin(name = "Ember", id = "EMBER", version = "1.0.0")
public class Ember {

    // Injects;
    @Inject Game game;

    // Singleton
    private static Ember instance;
    private static Serializer serializer = new Serializer();
    private static Backup backup = new Backup("backup/");

    // A map to store arenas and their games.
    private static HashMap<Arena, Task> arenas;

    /**
     * This method is called on server initialization.
     */
    @Subscribe
    private void onInitialization(InitializationEvent event) {
        Ember.instance = this;
        Ember.serializer = new Serializer();
        Ember.arenas = new HashMap<Arena, Task>();
    }

    /**
     * Get the Game.
     *
     * @return Sponge's game instance.
     */
    public static Game game() {
        return Ember.instance.game;
    }

    /**
     * Get the Serializer.
     *
     * @return Ember's serialization manager.
     */
    public static Serializer serializer() {
        return Ember.serializer;
    }

    /**
     * Get the backup manager.
     *
     * @return Ember's backup manager.
     */
    public static Backup backup() { return Ember.backup; }

    /**
     * Set the minigame to be played on an arena.
     *
     * @param arena the arena for the game to be played on.
     * @param minigame the game to be played.
     * @return true if the minigame was set successfully.
     * @throws Exception if the minigame was unable to override the currently running minigame.
     */
    public static boolean register(Arena arena, Minigame minigame) throws Exception {
        if (Ember.getArena(minigame).isPresent()) return false;

        // If the arena is already registered then..
        if (Ember.getArena(arena.getUniqueId()).isPresent()) {
            Ember.unregister(arena);
        }

        // If the minigame isn't null, then...
        if (minigame != null) {
            // We then register our new minigame to the EventHandler.
            if (minigame.events()) {
                Ember.instance.game.getEventManager().register(Ember.instance, minigame);
            }

            // Call an event so that the plugins know a minigame has started.
            Ember.instance.game.getEventManager().post(new MinigameStartedEvent(minigame));

            // We then create a new Task.
            Task task = Ember.instance.game.getScheduler().getTaskBuilder()
                    .name(arena.getName())
                    .delay(minigame.delay())
                    .interval(minigame.interval())
                    .execute(minigame)
                    .submit(Ember.instance);

            // We then register the task to be executed on the specified arena.
            Ember.arenas.put(arena, task);
        }

        // We return true to acknowlegde the task has been registered successfully.
        return true;
    }

    /**
     * Unregister an arena.
     *
     * @param arena The arena object.
     * @return true if the arena was removed successfully.
     * @throws Exception if the minigame was unable to override the currently running minigame.
     */
    public static boolean unregister(Arena arena) throws Exception {
        // Get the currently occupying Task.
        Task task = Ember.arenas.get(arena);

        // If the task exists...
        if (task != null) {
            // Call an event so that the plugins know a minigame is being stopped.
            boolean cancelled = Ember.instance.game.getEventManager().post(new MinigameStoppingEvent((Minigame) task.getRunnable()));

            if (cancelled)
                throw new Exception("Unable to override the currently running minigame.");

            // If the event isn't cancelled, we continue cancelling the currently
            // running minigame.
            task.cancel();

            // Call an event so that the plugins know a minigame has stopped.
            Ember.instance.game.getEventManager().post(new MinigameStoppedEvent((Minigame) task.getRunnable()));

            // Unregister the object from the EventManager.
            Ember.instance.game.getEventManager().unregister(task.getRunnable());

            // Remove the arena
            Ember.arenas.remove(arena);

            return true;
        }
        return false;
    }

    /**
     * Get an arena by it's identifier.
     *
     * @param id the arena's unique identifer.
     * @return the arena wrapped in an Optional.
     */
    public static Optional<Arena> getArena(UUID id) {
        for (Arena a : Ember.arenas.keySet()) {
            if (a.getUniqueId().equals(id)) return Optional.of(a);
        }
        return Optional.absent();
    }

    /**
     * Get an arena by it's running minigame.
     *
     * @param minigame the currently running minigame.
     * @return the arena wrapped in an Optional.
     */
    public static Optional<Arena> getArena(Minigame minigame) {
        for (Arena a : Ember.arenas.keySet()) {
            if (Ember.arenas.get(a).getRunnable().equals(minigame)) return Optional.of(a);
        }
        return Optional.absent();
    }

    /**
     * Get the minigame of a specific arena.
     *
     * @param arena the arena you wish to get the minigame.
     * @return the minigame wrapped in an Optional.
     */
    public static Optional<Minigame> getMinigame(Arena arena) {
        return (Ember.arenas.get(arena) != null) ? Optional.of((Minigame) Ember.arenas.get(arena).getRunnable()) : Optional.<Minigame>absent();
    }

    /**
     * Get all of the currently registered arenas.
     *
     * @return An ImmutableList of Arena objects.
     */
    public static ImmutableList<Arena> getArenas() {
        return ImmutableList.copyOf(Ember.arenas.keySet());
    }

    /**
     * Gets the Arena from the passed world.
     *
     * @param world The world a Arena is on.
     * @return Arena in that world.
     */
    public static ImmutableList<Arena> getArenas(World world){

        //A collection to store the arena temporarily.
        Collection<Arena> arenas = new ArrayList<Arena>();

        //Loop through all the arenas.
        for (Arena a : Ember.arenas.keySet()) {

            //If the arena's world equals the passed world.
            if (a.getSpawn().getWorld().equals(world)) {
                //add it to the collection.
                arenas.add(a);
            }
        }
        return ImmutableList.copyOf(arenas);
    }

    /**
     * Get all of the currently running minigames.
     *
     * @return An ImmutableList of Minigame objects.
     */
    public static ImmutableList<Minigame> getMinigames() {
        List<Minigame> games = new ArrayList<Minigame>();
        for (Task t : Ember.arenas.values()) {
            games.add((Minigame) t.getRunnable());
        }
        return ImmutableList.copyOf(games);
    }

    /**
     * Gets the minigames that are being played on the provided world.
     *
     * @param world The world that one or more minigames are being played on.
     * @return An ImmutableList containing the minigames for that world.
     */
    public static ImmutableList<Minigame> getMinigames(World world){
        //A collection to store the minigames temporarily.
        Collection<Minigame> minigames = new ArrayList<Minigame>();

        //Loop through the active minigames.
        for (Arena a : Ember.getArenas(world)) {
            //Get the minigame for the arena
            Optional<Minigame> minigame = Ember.getMinigame(a);
                
            //If the minigame is present
            if(minigame.isPresent()) {
                //If so then return the minigame.
                minigames.add(minigame.get());
            }

        }
        return ImmutableList.copyOf(minigames);
    }


    /**
     * A class to handle object serialization.
     */
    public static final class Serializer {

        // A set to store all of our type adapters.
        private HashMap<Class<?>, Class<? extends TypeAdapter>> adapters;

        /**
         * Serializer constructor.
         */
        protected Serializer() {
            this.adapters = new HashMap<Class<?>, Class<? extends TypeAdapter>>();

            this.register(Class.class, ClassAdapter.class);
            this.register(World.class, WorldAdapter.class);
        }

        /**
         * Register a TypeAdapter.
         *
         * @param object the object to adapt toward.
         * @param adapter the adapters class.
         * @param <T>
         * @return true if the adapters was registered successfully.
         */
        public <T extends TypeAdapter> boolean register(Class<?> object, Class<T> adapter) {
            if (!this.adapters.containsKey(object)) {
                this.adapters.put(object, adapter);
            }
            return this.adapters.containsKey(object);
        }

        /**
         * Unregister a TypeAdapter.
         *
         * @param <T>
         * @return true if the adapters was unregistered successfully.
         */
        public <T extends TypeAdapter> boolean unregister(Class<?> object) {
            if (!this.adapters.containsKey(object)) {
                this.adapters.remove(object);
            }
            return this.adapters.containsKey(object);
        }

        /**
         * Get the GsonBuilder.
         *
         * @return GsonBuilder instance.
         * @throws IllegalAccessException if a registered adapter is not accessible.
         * @throws InstantiationException if a registered adapter cannot be instantiated.
         */
        public Gson gson() throws InstantiationException, IllegalAccessException {
            return this.gson(null);
        }

        /**
         * Get the GsonBuilder.
         *
         * @param builder your custom GsonBuilder instance.
         * @return GsonBuilder instance.
         * @throws IllegalAccessException if a registered adapter is not accessible.
         * @throws InstantiationException if a registered adapter cannot be instantiated.
         */
        public Gson gson(GsonBuilder builder) throws IllegalAccessException, InstantiationException {
            GsonBuilder b = (builder != null) ? builder : new GsonBuilder();

            for (Map.Entry<Class<?>, Class<? extends TypeAdapter>> entry : this.adapters.entrySet()) {
                b.registerTypeHierarchyAdapter(entry.getKey(), entry.getValue().newInstance());
            }
            return b.create();
        }

        /**
         * Get the object type from the JSON.
         *
         * @param json the json data.
         * @return the relevant class.
         * @throws ClassNotFoundException if the class wasn't found.
         */
        public Class getType(String json) throws ClassNotFoundException {
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(json);
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("type")) {
                    element = obj.get("type");
                    if (element.isJsonObject()) {
                        obj = element.getAsJsonObject();
                        if (obj.has("class")) {
                            JsonPrimitive value = obj.get("class").getAsJsonPrimitive();
                            if (value.isString()) {
                                return Class.forName(value.getAsString());
                            }
                        }
                    }
                }
            }
            throw new ClassNotFoundException("The JSON data provided doesn't contain a valid \"type\" object.");
        }

        /**
         * Attempts to get and load the correct object from the JSON data.
         *
         * @param json the json data.
         * @return The object.
         * @throws ClassNotFoundException if the object type wasn't found.
         * @throws IllegalAccessException if a registered adapter is not accessible.
         * @throws InstantiationException if a registered adapter cannot be instantiated.
         */
        public Object getAndLoad(String json) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
            return this.gson().fromJson(json, this.getType(json));
        }

    }

    /**
     * Ember's backup manager.
     * Used for loading and storing worlds.
     */
    public static final class Backup {

        private File backupDirectory;   //Directory for the backups
        private String worldsDirectory; //Directory where the worlds are located

        /**
         * Constructs ember's BackupManager
         *
         * @param path The destination folder for the backups
         */
        public Backup(String path) {

            //Set the destination folder.
            backupDirectory = new File(path);

            //If the backupdirectory does not exist
            if (!backupDirectory.exists()) {

                //Then create a new directory
                backupDirectory.mkdir();
            }

            //If it still doesnt exist or it isn't a directory then throw an error
            if (!backupDirectory.exists() || !backupDirectory.isDirectory()) {

                //Throw a runtime exception if the directory doesn't exist.
                //Throw a runtime exception if the destination isn't a directory.
                throw new IllegalBackupDestination("Backup destination is not a folder or couldn't be created! " + backupDirectory.getAbsolutePath());
            }

            try {

                //A world gets called after it's folder name. So if `worldName` corresponds to a world then
                //we can just assume that there's a folder called worlds.

                //Build a loader and crate a configuratioNode from that. then get the 'level-name' from the server.properties
                HoconConfigurationLoader loader = HoconConfigurationLoader.builder().setFile(new File("server.properties")).build();
                ConfigurationNode node = loader.load();

                //Create the serverDir File from that.
                worldsDirectory = (String)node.getNode("level-name").getValue();

            }catch ( IOException e ) {

                //Throw a runtime exception if we can't load the server.properties file.
                throw new RuntimeException( "Unable to load server.properties!\n" + e.getMessage() );
            }

        }

        /**
         * Returns a full list of files prefixed by their directories relative to
         * the starting directory.
         *
         * @param source The starting directory
         * @return A list of files prefixed with their relative directories
         */
        private String[] generateFileList( File source ) {

            //Create a list to store the entries temporarily
            List<String> files = new ArrayList<String>();

            //Iterate through the current directory to add files.
            for ( File file : source.listFiles() ) {

                //If the file is a directory then call this function again
                if (file.isDirectory()) {

                    //Loop through the received files and add them
                    for ( String subFile : generateFileList(file) ) {

                        //Add the file.
                        files.add(subFile);
                    }

                }else {

                    //Get the world's path
                    String path = file.getPath();

                    //Remove the worlds directory from the pathname,
                    //so that the zipEntry does not prefix everything with the worldfolder
                    files.add(path.substring(worldsDirectory.length()+1,path.length()));

                }

            }

            //return an array.
            return files.toArray( new String[files.size()] );
        }

        /**
         * Creates a compressed backup from the specified source directory and writes it to
         * the specified destinationFolder that is prefixed by the default backupDirectory
         *
         * @param source The directory to create a backup from
         * @param destinationFolder The directory to write it to
         */
        private void createCompressedBackup( String source, String destinationFolder ) {

            //Prefix the world with the world directory.
            File sourceFile = new File( worldsDirectory + File.separator + source );

            //New destination directory: e.g. "backup/worlds/..."
            File destinationPath = new File(backupDirectory.getAbsolutePath().concat(File.separator + destinationFolder));

            //If the destination directory does not exist
            //then create a new one.
            if (!destinationPath.exists()) {
                destinationPath.mkdir();
            }

            //If the destination is not a directory
            //then throw a runtime error.
            if (!backupDirectory.exists() || !backupDirectory.isDirectory()) {

                //Throw a runtime exception if the directory doesn't exist.
                //Throw a runtime exception if the destination isn't a directory.
                throw new IllegalBackupDestination("Backup destination is not a folder or does not exist! " + backupDirectory.getAbsolutePath());
            }

            //If the source folder/file does not exist
            //then throw a runtime error
            if (!sourceFile.exists()) {

                //Throw a runtime exception if the source does not exist.
                throw new IllegalBackupDestination("Backup source does not exist! " + sourceFile.getAbsolutePath());
            }

            //Catch any exception that might happen.
            try {

                //Create the ZipOutputStream
                ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destinationPath.getPath().concat(File.separator + sourceFile.getName() + ".zip")));

                //Variables for the reading and writing to the zipentry
                byte[] buffer = new byte[1024]; //Buffer used to store data temporarily
                int len=0;                      //Length that was read.

                String[] filesToCopy = generateFileList(sourceFile);

                //Iterate through all the files and copy them
                for (String f : filesToCopy) {

                    // Create a new zipEntry and add it to the outputStream but remove the worldname beginning in the zip file
                    // so we don't end up with a zip called "World.zip" that contains a directory called "World"
                    ZipEntry ze = new ZipEntry(f.substring(sourceFile.getName().length()+1));
                    zipOutputStream.putNextEntry(ze);

                    //Open the file to be written to the zip
                    FileInputStream fileInputStream = new FileInputStream(worldsDirectory + File.separator + f);

                    //Loop and write the data we have available
                    while ( ( len = fileInputStream.read(buffer, 0, buffer.length) ) > 0 ){

                        //Write the read data to the zip file.
                        zipOutputStream.write(buffer, 0, len);

                    }

                    //Close the stream
                    fileInputStream.close();
                    zipOutputStream.closeEntry();

                }

                //Close the zip file.
                zipOutputStream.close();

            }catch(IOException e){

                //Print the exception to the console.
                e.printStackTrace();
            }

        }

        /**
         * Creates a backup of the chosen world.
         * Make sure the chosen world is unloaded!
         *
         * @Param worldName The world's name to backup.
         */
        public void backupWorld(String worldName) {

            //Try to get the world to backup.
            Optional<World> worldOptional = Ember.game().getServer().getWorld(worldName);

            //If the world does not exist throw an exception
            if (!worldOptional.isPresent()) {

                throw new RuntimeException(String.format("Cannot backup %s, world does not exist!", worldName));
            }

            //If the world is still loaded throw an exception.
//            if( worldOptional.get().isLoaded() ) {
//
//                throw new RuntimeException(String.format("Cannot backup %s, world is still loaded!", worldName));
//            }

            createCompressedBackup( worldName, "Worlds");

        }

        /**
         * Loads a world from the backup folder and copies it in the worlds folder under the same name.
         *
         * @param backupName The backup worldname
         */
        public void loadWorld(String backupName) {
            loadWorld(backupName, backupName, false);
        }

        /**
         * Loads a world from the backup folder and copies it in the worlds folder under the same name.
         *
         * @param backupName The world name in the backups
         * @param worldName The destination world name.
         */
        public void loadWorld(String backupName, String worldName) {
            loadWorld(backupName, worldName, false);
        }

        /**
         * Loads a world from the backup folder and copies it in the worlds folder.
         *
         * @param backupName The world name in the backups
         * @param worldName The destination world name.
         * @param overwrite Overwrite destination if it already exists?
         */
        public void loadWorld(String backupName, String worldName, boolean overwrite) {

            //Check if there already is a world with the destination name.
            Optional<World> worldOptional = Ember.game().getServer().getWorld(worldName);

            if (worldOptional.isPresent()) {

                if (!overwrite) {

                    //If we dont wan't to overwrite the existing file, throw an exception.
                    throw new RuntimeException("Backup loading failed! Destination world already exists! Enable overwrite to overwrite");
                }else{

                    //Otherwise remove the existing world.
                    File existingWorld = new File(worldsDirectory + File.separator + worldName);

                    // Make sure that the folder we're about to remove contains the two essential world files
                    // so that we can't remove the wrong folder.
                    if( Arrays.asList(existingWorld.listFiles()).contains("level.dat") && Arrays.asList(existingWorld.listFiles()).contains("level_sponge.dat") ) {

                        //Remove the world folder.
                        existingWorld.delete();
                    }
                }
            }

            File worldDir = new File(worldsDirectory + File.separator + worldName);

            if (!worldDir.exists()) {
                worldDir.mkdir();
            }

            try {

                //Try to load the zipfile.
                ZipFile zipFile = new ZipFile(backupDirectory + File.separator + "worlds" + File.separator + backupName + ".zip");

                //Get all the entries in the zip file
                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                //Variables for reading and writing
                byte[] buffer = new byte[1024];
                int len=0;

                //While there are more entries.
                while (entries.hasMoreElements()){

                    //Get the next entry
                    ZipEntry ze = entries.nextElement();

                    //Get the entry as a file.
                    File entryFile = new File(worldsDirectory + File.separator + worldName + File.separator + ze.getName());

                    if (ze.isDirectory()) {
                        entryFile.mkdirs();
                    } else {
                        entryFile.getParentFile().mkdirs();
                    }

                    //Create the output stream for the new file and the input stream from the zipfile
                    FileOutputStream fileOutputStream = new FileOutputStream(entryFile);
                    InputStream entryInputStream = zipFile.getInputStream(ze);

                    //While there is something to read then write it to the output file
                    while ( (len=entryInputStream.read( buffer, 0, buffer.length )) > 0 ) {
                        fileOutputStream.write(buffer , 0, buffer.length);
                    }

                    //Close all the streams.
                    entryInputStream.close();
                    fileOutputStream.close();

                }

                //Close the zip file.
                zipFile.close();


            }catch( IOException e ) {
                e.printStackTrace();
            }

        }

    }

}
