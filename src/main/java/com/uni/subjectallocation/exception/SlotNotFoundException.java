package com.uni.subjectallocation.exception;

public class SlotNotFoundException extends RuntimeException {

    public SlotNotFoundException(String slot) {
        super("Slot not found: " + slot);
    }
}
