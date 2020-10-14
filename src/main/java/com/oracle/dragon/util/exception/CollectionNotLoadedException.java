package com.oracle.dragon.util.exception;

public class CollectionNotLoadedException extends DSException {
    public CollectionNotLoadedException(String collectionName, Throwable t) {
        super(ErrorCode.CollectionNotLoaded,String.format("Collection %s files could not be loaded!", collectionName),t);
    }
}
