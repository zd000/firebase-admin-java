package com.google.firebase.remoteconfig.internal;

import com.google.api.client.util.Key;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.internal.Nullable;
import com.google.firebase.remoteconfig.RemoteConfigErrorCode;

import java.util.List;
import java.util.Map;

/**
 * The DTO for parsing error responses from the Remote Config service.
 */
public class RemoteConfigServiceErrorResponse {

  private static final Map<String, RemoteConfigErrorCode> MESSAGING_ERROR_CODES =
          ImmutableMap.<String, RemoteConfigErrorCode>builder()
                  .put("INTERNAL", RemoteConfigErrorCode.INTERNAL)
                  .build();

  private static final String RC_ERROR_TYPE =
          "type.googleapis.com/google.firebase.fcm.v1.FcmError";

  @Key("error")
  private Map<String, Object> error;

  public String getStatus() {
    if (error == null) {
      return null;
    }

    return (String) error.get("status");
  }

  @Nullable
  public RemoteConfigErrorCode getRemoteConfigErrorCode() {
    if (error == null) {
      return null;
    }

    Object details = error.get("details");
    if (details instanceof List) {
      for (Object detail : (List<?>) details) {
        if (detail instanceof Map) {
          Map<?, ?> detailMap = (Map<?, ?>) detail;
          if (RC_ERROR_TYPE.equals(detailMap.get("@type"))) {
            String errorCode = (String) detailMap.get("errorCode");
            return MESSAGING_ERROR_CODES.get(errorCode);
          }
        }
      }
    }

    return null;
  }

  @Nullable
  public String getErrorMessage() {
    if (error != null) {
      return (String) error.get("message");
    }

    return null;
  }
}
