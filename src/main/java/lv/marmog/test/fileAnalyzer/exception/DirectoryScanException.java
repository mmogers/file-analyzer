package lv.marmog.test.fileAnalyzer.exception;

public class DirectoryScanException extends Exception{
	public DirectoryScanException(String message) {
		super(message);
	}

	public DirectoryScanException(String message, Throwable cause) {
		super(message, cause);
	}
}
