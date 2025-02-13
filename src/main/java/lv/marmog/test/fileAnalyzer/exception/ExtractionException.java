package lv.marmog.test.fileAnalyzer.exception;

public class ExtractionException extends CustomException {

	public ExtractionException(String message) {
		super(message);
	}

	public ExtractionException(String message, Throwable cause) {
		super(message, cause);
	}
}
