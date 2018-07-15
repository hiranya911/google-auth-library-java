/*
 * Copyright 2015, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.google.auth.oauth2;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.util.Preconditions;
import com.google.auth.http.HttpTransportFactory;

import java.io.IOException;
import java.io.InputStream;

import java.util.Collection;

/**
 * Base type for credentials for authorizing calls to Google APIs using OAuth2.
 */
public class GoogleCredentials extends OAuth2Credentials {

  private static final long serialVersionUID = -1522852442442473691L;
  static final String USER_FILE_TYPE = "authorized_user";
  static final String SERVICE_ACCOUNT_FILE_TYPE = "service_account";

  private static final DefaultCredentialsProvider defaultCredentialsProvider =
      new DefaultCredentialsProvider();

  /**
   * Returns the credentials instance from the given access token.
   *
   * @param accessToken the access token
   * @return the credentials instance
   * @deprecated Use {@link #create(AccessToken)} instead. This method will be deleted in a later
   *             version.
   */
  @Deprecated
  public static GoogleCredentials of(AccessToken accessToken) {
    return create(accessToken);
  }

  /**
   * Returns the credentials instance from the given access token.
   *
   * @param accessToken the access token
   * @return the credentials instance
   */
  public static GoogleCredentials create(AccessToken accessToken) {
    return GoogleCredentials.newBuilder().setAccessToken(accessToken).build();
  }

  /**
   * Returns the Application Default Credentials.
   *
   * <p>Returns the Application Default Credentials which are used to identify and authorize the
   * whole application. The following are searched (in order) to find the Application Default
   * Credentials:
   * <ol>
   *   <li>Credentials file pointed to by the {@code GOOGLE_APPLICATION_CREDENTIALS} environment
   *   variable</li>
   *   <li>Credentials provided by the Google Cloud SDK
   *   {@code gcloud auth application-default login} command</li>
   *   <li>Google App Engine built-in credentials</li>
   *   <li>Google Cloud Shell built-in credentials</li>
   *   <li>Google Compute Engine built-in credentials</li>
   * </ol>
   *
   * @return the credentials instance.
   * @throws IOException if the credentials cannot be created in the current environment.
   */
  public static GoogleCredentials getApplicationDefault() throws IOException {
    return getApplicationDefault(OAuth2Utils.HTTP_TRANSPORT_FACTORY);
  }

  /**
   * Returns the Application Default Credentials.
   *
   * <p>Returns the Application Default Credentials which are used to identify and authorize the
   * whole application. The following are searched (in order) to find the Application Default
   * Credentials:
   * <ol>
   *   <li>Credentials file pointed to by the {@code GOOGLE_APPLICATION_CREDENTIALS} environment
   *   variable</li>
   *   <li>Credentials provided by the Google Cloud SDK
   *   {@code gcloud auth application-default login} command</li>
   *   <li>Google App Engine built-in credentials</li>
   *   <li>Google Cloud Shell built-in credentials</li>
   *   <li>Google Compute Engine built-in credentials</li>
   * </ol>
   *
   * @param transportFactory HTTP transport factory, creates the transport used to get access
   *        tokens.
   * @return the credentials instance.
   * @throws IOException if the credentials cannot be created in the current environment.
   **/
  public static GoogleCredentials getApplicationDefault(HttpTransportFactory transportFactory)
      throws IOException {
    Preconditions.checkNotNull(transportFactory);
    return defaultCredentialsProvider.getDefaultCredentials(transportFactory);
  }

  /**
   * Returns credentials defined by a JSON file stream.
   *
   * <p>The stream can contain a Service Account key file in JSON format from the Google Developers
   * Console or a stored user credential using the format supported by the Cloud SDK.</p>
   *
   * @param credentialsStream the stream with the credential definition.
   * @return the credential defined by the credentialsStream.
   * @throws IOException if the credential cannot be created from the stream.
   **/
  public static GoogleCredentials fromStream(InputStream credentialsStream) throws IOException {
    return fromStream(credentialsStream, OAuth2Utils.HTTP_TRANSPORT_FACTORY);
  }

  /**
   * Returns credentials defined by a JSON file stream.
   *
   * <p>The stream can contain a Service Account key file in JSON format from the Google Developers
   * Console or a stored user credential using the format supported by the Cloud SDK.</p>
   *
   * @param credentialsStream the stream with the credential definition.
   * @param transportFactory HTTP transport factory, creates the transport used to get access
   *        tokens.
   * @return the credential defined by the credentialsStream.
   * @throws IOException if the credential cannot be created from the stream.
   **/
  public static GoogleCredentials fromStream(InputStream credentialsStream,
      HttpTransportFactory transportFactory) throws IOException {
    Preconditions.checkNotNull(credentialsStream);
    Preconditions.checkNotNull(transportFactory);

    JsonFactory jsonFactory = OAuth2Utils.JSON_FACTORY;
    JsonObjectParser parser = new JsonObjectParser(jsonFactory);
    GenericJson fileContents = parser.parseAndClose(
        credentialsStream, OAuth2Utils.UTF_8, GenericJson.class);

    String fileType = (String) fileContents.get("type");
    if (fileType == null) {
      throw new IOException("Error reading credentials from stream, 'type' field not specified.");
    }
    if (USER_FILE_TYPE.equals(fileType)) {
      return UserCredentials.fromJson(fileContents, transportFactory);
    }
    if (SERVICE_ACCOUNT_FILE_TYPE.equals(fileType)) {
      return ServiceAccountCredentials.fromJson(fileContents, transportFactory);
    }
    throw new IOException(String.format(
        "Error reading credentials from stream, 'type' value '%s' not recognized."
            + " Expecting '%s' or '%s'.",
        fileType, USER_FILE_TYPE, SERVICE_ACCOUNT_FILE_TYPE));
  }

  /**
   * Default constructor.
   **/
  protected GoogleCredentials() {
    this(null);
  }

  /**
   * Constructor with explicit access token.
   *
   * @param accessToken Initial or temporary access token.
   * @deprecated Use {@link #create(AccessToken)} instead. This constructor will either be deleted
   *             or made protected/private in a later version.
   **/
  @Deprecated
  public GoogleCredentials(AccessToken accessToken) {
    super(accessToken);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  /**
   * Indicates whether the credentials require scopes to be specified via a call to
   * {link GoogleCredentials#createScoped} before use.
   */
  public boolean createScopedRequired() {
    return false;
  }

  /**
   * If the credentials support scopes, creates a copy of the the identity with the specified
   * scopes; otherwise, returns the same instance.
   */
  public GoogleCredentials createScoped(Collection<String> scopes) {
    return this;
  }

  /**
   * If the credentials support domain-wide delegation, creates a copy
   * of the identity so that it impersonates the specified user;
   * otherwise, returns the same instance.
   */
  public GoogleCredentials createDelegated(String user) {
    return this;
  }

  public static class Builder extends OAuth2Credentials.Builder {
    protected Builder() {}

    protected Builder(GoogleCredentials credentials) {
      setAccessToken(credentials.getAccessToken());
    }

    public GoogleCredentials build() {
      return new GoogleCredentials(getAccessToken());
    }

    @Override
    public Builder setAccessToken(AccessToken token) {
      super.setAccessToken(token);
      return this;
    }
  }
}
