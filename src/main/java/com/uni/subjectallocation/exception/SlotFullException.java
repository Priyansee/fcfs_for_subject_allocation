package com.uni.subjectallocation.exception;

public class SlotFullException extends RuntimeException {

    public SlotFullException() {
        super("Slot Full");
    }
}
