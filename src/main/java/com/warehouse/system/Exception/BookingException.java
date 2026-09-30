package com.warehouse.system.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class BookingException extends RuntimeException {
    public BookingException(String message) {
        super(message);
    }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public static class Validation extends BookingException { public Validation(String m) { super(m); } }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  public static class NotFound extends BookingException { public NotFound(String m) { super(m); } }

  @ResponseStatus(HttpStatus.FORBIDDEN)
  public static class Forbidden extends BookingException { public Forbidden(String m) { super(m); } }

  /** Unit already booked / held for an overlapping period */
  @ResponseStatus(HttpStatus.CONFLICT)
  public static class Conflict extends BookingException { public Conflict(String m) { super(m); } }

  @ResponseStatus(HttpStatus.CONFLICT)
  public static class InvalidState extends BookingException { public InvalidState(String m) { super(m); } }

  @ResponseStatus(HttpStatus.GONE)
  public static class HoldExpired extends BookingException { public HoldExpired(String m) { super(m); } }

  @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
  public static class LimitExceeded extends BookingException { public LimitExceeded(String m) { super(m); } }

  /** Couldn't get the lock in time – client may retry */
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public static class Busy extends BookingException { public Busy(String m) { super(m); } }
}
