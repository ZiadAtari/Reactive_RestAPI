package ziadatari.ReactiveAPI.exception;

/**
 * Custom runtime exception used within the service layer to signal business
 * logic errors.
 * Always carries an ErrorCode that can be mapped to an API response.
 */
public class ServiceException extends RuntimeException {

  /** The specific error code associated with this business exception. */
  private final ErrorCode errorCode;

  /**
   * Constructs a ServiceException with a specific ErrorCode.
   *
   * @param errorCode the ErrorCode associated with this exception
   */
  public ServiceException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  /**
   * Constructs a ServiceException with a specific ErrorCode and a custom message.
   * Use this when the default message of the ErrorCode needs more context.
   *
   * @param errorCode     the ErrorCode associated with this exception
   * @param customMessage a specific message for this failure
   */
  public ServiceException(ErrorCode errorCode, String customMessage) {
    super(customMessage);
    this.errorCode = errorCode;
  }

  /**
   * Gets the ErrorCode associated with this exception.
   *
   * @return the ErrorCode
   */
  public ErrorCode getErrorCode() {
    return errorCode;
  }

}
