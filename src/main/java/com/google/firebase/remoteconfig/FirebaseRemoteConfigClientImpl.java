/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.remoteconfig;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponseInterceptor;
import com.google.api.client.json.JsonFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.ImplFirebaseTrampolines;
import com.google.firebase.IncomingHttpResponse;
import com.google.firebase.internal.AbstractPlatformErrorHandler;
import com.google.firebase.internal.ApiClientUtils;
import com.google.firebase.internal.ErrorHandlingHttpClient;
import com.google.firebase.internal.HttpRequestInfo;
import com.google.firebase.internal.SdkUtils;
import com.google.firebase.remoteconfig.internal.RemoteConfigServiceErrorResponse;

import java.io.IOException;
import java.util.Map;

/**
 * A helper class for interacting with Firebase Remote Config service.
 */
public class FirebaseRemoteConfigClientImpl implements FirebaseRemoteConfigClient {

  private static final String RC_URL = "https://firebaseremoteconfig.googleapis.com/v1/projects/%s/remoteConfig";

  private static final Map<String, String> COMMON_HEADERS =
          ImmutableMap.of(
                  "X-Firebase-Client", "fire-admin-java/" + SdkUtils.getVersion()
          );

  private final String rcSendUrl;
  private final HttpRequestFactory requestFactory;
  private final HttpRequestFactory childRequestFactory;
  private final JsonFactory jsonFactory;
  private final HttpResponseInterceptor responseInterceptor;
  private final FirebaseRemoteConfigClientImpl.RemoteConfigErrorHandler errorHandler;
  private final ErrorHandlingHttpClient<FirebaseRemoteConfigException> httpClient;

  private FirebaseRemoteConfigClientImpl(FirebaseRemoteConfigClientImpl.Builder builder) {
    checkArgument(!Strings.isNullOrEmpty(builder.projectId));
    this.rcSendUrl = String.format(RC_URL, builder.projectId);
    this.requestFactory = checkNotNull(builder.requestFactory);
    this.childRequestFactory = checkNotNull(builder.childRequestFactory);
    this.jsonFactory = checkNotNull(builder.jsonFactory);
    this.responseInterceptor = builder.responseInterceptor;
    this.errorHandler = new FirebaseRemoteConfigClientImpl.RemoteConfigErrorHandler(
            this.jsonFactory);
    this.httpClient = new ErrorHandlingHttpClient<>(requestFactory, jsonFactory, errorHandler)
            .setInterceptor(responseInterceptor);
  }

  @VisibleForTesting
  String getRcSendUrl() {
    return rcSendUrl;
  }

  @VisibleForTesting
  HttpRequestFactory getRequestFactory() {
    return requestFactory;
  }

  @VisibleForTesting
  HttpRequestFactory getChildRequestFactory() {
    return childRequestFactory;
  }

  @VisibleForTesting
  JsonFactory getJsonFactory() {
    return jsonFactory;
  }

  @Override
  public RemoteConfigTemplate getTemplate() throws FirebaseRemoteConfigException {
    HttpRequestInfo request =
            HttpRequestInfo.buildGetRequest(rcSendUrl)
                    .addAllHeaders(COMMON_HEADERS);
    IncomingHttpResponse response = httpClient.send(request);
    System.out.println(response.getHeaders());
    //RemoteConfigTemplate parsed = httpClient.sendAndParse(
    //        request, RemoteConfigTemplate.class);
    RemoteConfigTemplate parsed = httpClient.parse(response, RemoteConfigTemplate.class);
    parsed.setETag(response.getHeaders().get("etag").toString());
    return parsed;
  }

  static FirebaseRemoteConfigClientImpl fromApp(FirebaseApp app) {
    String projectId = ImplFirebaseTrampolines.getProjectId(app);
    checkArgument(!Strings.isNullOrEmpty(projectId),
            "Project ID is required to access Remote Config service. Use a service "
                    + "account credential or set the project ID explicitly via FirebaseOptions. "
                    + "Alternatively you can also set the project ID via the GOOGLE_CLOUD_PROJECT "
                    + "environment variable.");
    return FirebaseRemoteConfigClientImpl.builder()
            .setProjectId(projectId)
            .setRequestFactory(ApiClientUtils.newAuthorizedRequestFactory(app))
            .setChildRequestFactory(ApiClientUtils.newUnauthorizedRequestFactory(app))
            .setJsonFactory(app.getOptions().getJsonFactory())
            .build();
  }

  static FirebaseRemoteConfigClientImpl.Builder builder() {
    return new FirebaseRemoteConfigClientImpl.Builder();
  }

  static final class Builder {

    private String projectId;
    private HttpRequestFactory requestFactory;
    private HttpRequestFactory childRequestFactory;
    private JsonFactory jsonFactory;
    private HttpResponseInterceptor responseInterceptor;

    private Builder() {
    }

    FirebaseRemoteConfigClientImpl.Builder setProjectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    FirebaseRemoteConfigClientImpl.Builder setRequestFactory(HttpRequestFactory requestFactory) {
      this.requestFactory = requestFactory;
      return this;
    }

    FirebaseRemoteConfigClientImpl.Builder setChildRequestFactory(
            HttpRequestFactory childRequestFactory) {
      this.childRequestFactory = childRequestFactory;
      return this;
    }

    FirebaseRemoteConfigClientImpl.Builder setJsonFactory(JsonFactory jsonFactory) {
      this.jsonFactory = jsonFactory;
      return this;
    }

    FirebaseRemoteConfigClientImpl.Builder setResponseInterceptor(
            HttpResponseInterceptor responseInterceptor) {
      this.responseInterceptor = responseInterceptor;
      return this;
    }

    FirebaseRemoteConfigClientImpl build() {
      return new FirebaseRemoteConfigClientImpl(this);
    }
  }

  private static class RemoteConfigErrorHandler
          extends AbstractPlatformErrorHandler<FirebaseRemoteConfigException> {

    private RemoteConfigErrorHandler(JsonFactory jsonFactory) {
      super(jsonFactory);
    }

    @Override
    protected FirebaseRemoteConfigException createException(FirebaseException base) {
      String response = getResponse(base);
      RemoteConfigServiceErrorResponse parsed = safeParse(response);
      return FirebaseRemoteConfigException.withRemoteConfigErrorCode(
              base, parsed.getRemoteConfigErrorCode());
    }

    private String getResponse(FirebaseException base) {
      if (base.getHttpResponse() == null) {
        return null;
      }

      return base.getHttpResponse().getContent();
    }

    private RemoteConfigServiceErrorResponse safeParse(String response) {
      if (!Strings.isNullOrEmpty(response)) {
        try {
          return jsonFactory.createJsonParser(response)
                  .parseAndClose(RemoteConfigServiceErrorResponse.class);
        } catch (IOException ignore) {
          // Ignore any error that may occur while parsing the error response. The server
          // may have responded with a non-json payload.
        }
      }

      return new RemoteConfigServiceErrorResponse();
    }
  }
}