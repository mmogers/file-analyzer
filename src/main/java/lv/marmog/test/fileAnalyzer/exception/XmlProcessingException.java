package lv.marmog.test.fileAnalyzer.exception;

public class XmlProcessingException extends CustomException{
	public XmlProcessingException(String message) {
		super(message);
	}

	public XmlProcessingException(String message, Throwable cause) {
		super(message, cause);
	}
}
