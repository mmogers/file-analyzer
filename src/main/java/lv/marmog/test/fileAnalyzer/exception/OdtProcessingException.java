package lv.marmog.test.fileAnalyzer.exception;

public class OdtProcessingException extends Exception{
	public OdtProcessingException(String message) {
		super(message);
	}

	public OdtProcessingException(String message, Throwable cause) {
		super(message, cause);
	}
}
