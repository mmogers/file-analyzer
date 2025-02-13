package lv.marmog.test.fileAnalyzer.exception;

public class InvalidFolderException extends Exception{
	public InvalidFolderException(String message) {
		super(message);
	}

	public InvalidFolderException(String message, Throwable cause) {
		super(message, cause);
	}
}
