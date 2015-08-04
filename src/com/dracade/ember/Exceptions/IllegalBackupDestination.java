package com.dracade.ember.exceptions;

public class IllegalBackupDestination extends RuntimeException {

    /**
     * Create a new IllegalBackupDestination.
     *
     * @param message
     */
    public IllegalBackupDestination(String message){
        super(message);
    }

}
