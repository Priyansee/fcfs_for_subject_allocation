package com.uni.subjectallocation.exception;

public class AlreadyEnrolledException extends RuntimeException {

    public AlreadyEnrolledException() {
        super("Already Enrolled");
    }
}
