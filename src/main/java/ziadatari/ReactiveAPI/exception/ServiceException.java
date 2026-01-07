package ziadatari.ReactiveAPI.exception;

public class ServiceException extends RuntimeException {

  private final ErrorCode errorCode;

  public ServiceException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  //Allow overriding message if needed
  public ServiceException(ErrorCode errorCode, String customMessage) {
    super(customMessage);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

}
