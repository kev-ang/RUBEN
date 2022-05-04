package at.sti2.exception;

public class MemoryLimitExceededException extends RuntimeException {

    public MemoryLimitExceededException(String message) {
        super(message);
    }
}
