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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Clock;
import com.google.auth.TestUtils;
import com.google.auth.http.AuthHttpConstants;
import com.google.auth.oauth2.GoogleCredentialsTest.MockHttpTransportFactory;
import com.google.auth.oauth2.GoogleCredentialsTest.MockTokenServerTransportFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Test case for {@link UserCredentials}.
 */
@RunWith(JUnit4.class)
public class UserCredentialsTest extends BaseSerializationTest {

  private static final String CLIENT_SECRET = "jakuaL9YyieakhECKL2SwZcu";
  private static final String CLIENT_ID = "ya29.1.AADtN_UtlxN3PuGAxrN2XQnZTVRvDyVWnYq4I6dws";
  private static final String REFRESH_TOKEN = "1/Tl6awhpFjkMkSJoj1xsli0H2eL5YsMgU_NKPY2TyGWY";
  private static final String ACCESS_TOKEN = "1/MkSJoj1xsli0AccessToken_NKPY2";
  private static final Collection<String> SCOPES = Collections.singletonList("dummy.scope");
  private static final URI CALL_URI = URI.create("http://googleapis.com/testapi/v1/foo");

  @Test(expected = IllegalStateException.class)
  public void constructor_accessAndRefreshTokenNull_throws() {
    UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .build();
  }

  @Test
  public void constructor_storesRefreshToken() {
    UserCredentials credentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .build();
    assertEquals(REFRESH_TOKEN, credentials.getRefreshToken());
  }

  @Test
  public void createScoped_same() {
    UserCredentials userCredentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .build();
    assertSame(userCredentials, userCredentials.createScoped(SCOPES));
  }

  @Test
  public void createScopedRequired_false() {
    UserCredentials userCredentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .build();
    assertFalse(userCredentials.createScopedRequired());
  }

  @Test
  public void fromJson_hasAccessToken() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);
    GenericJson json = writeUserJson(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN);

    GoogleCredentials credentials = UserCredentials.fromJson(json, transportFactory);

    Map<String, List<String>> metadata = credentials.getRequestMetadata(CALL_URI);
    TestUtils.assertContainsBearerToken(metadata, ACCESS_TOKEN);
  }

  @Test
  public void getRequestMetadata_initialToken_hasAccessToken() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    UserCredentials userCredentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(transportFactory)
        .build();

    Map<String, List<String>> metadata = userCredentials.getRequestMetadata(CALL_URI);

    TestUtils.assertContainsBearerToken(metadata, ACCESS_TOKEN);
  }

  @Test
  public void getRequestMetadata_initialTokenRefreshed_throws() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    UserCredentials userCredentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(transportFactory)
        .build();

    try {
      userCredentials.refresh();
      fail("Should not be able to refresh without refresh token.");
    } catch (IllegalStateException expected) {
      // Expected
    }
  }

  @Test
  public void getRequestMetadata_fromRefreshToken_hasAccessToken() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);
    UserCredentials userCredentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setHttpTransportFactory(transportFactory)
        .build();

    Map<String, List<String>> metadata = userCredentials.getRequestMetadata(CALL_URI);

    TestUtils.assertContainsBearerToken(metadata, ACCESS_TOKEN);
  }

  @Test
  public void getRequestMetadata_customTokenServer_hasAccessToken() throws IOException {
    final URI TOKEN_SERVER = URI.create("https://foo.com/bar");
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);
    transportFactory.transport.setTokenServerUri(TOKEN_SERVER);
    UserCredentials userCredentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setHttpTransportFactory(transportFactory)
        .setTokenServerUri(TOKEN_SERVER)
        .build();

    Map<String, List<String>> metadata = userCredentials.getRequestMetadata(CALL_URI);

    TestUtils.assertContainsBearerToken(metadata, ACCESS_TOKEN);
  }

  @Test
  public void equals_true() throws IOException {
    final URI tokenServer = URI.create("https://foo.com/bar");
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    UserCredentials credentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(transportFactory)
        .setTokenServerUri(tokenServer)
        .build();
    UserCredentials otherCredentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(transportFactory)
        .setTokenServerUri(tokenServer)
        .build();
    assertTrue(credentials.equals(otherCredentials));
    assertTrue(otherCredentials.equals(credentials));
  }

  @Test
  public void equals_false_clientId() throws IOException {
    final URI tokenServer1 = URI.create("https://foo1.com/bar");
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    UserCredentials credentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(httpTransportFactory)
        .setTokenServerUri(tokenServer1)
        .build();
    UserCredentials otherCredentials = UserCredentials.newBuilder()
        .setClientId("other client id")
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(httpTransportFactory)
        .setTokenServerUri(tokenServer1)
        .build();
    assertFalse(credentials.equals(otherCredentials));
    assertFalse(otherCredentials.equals(credentials));
  }

  @Test
  public void equals_false_clientSecret() throws IOException {
    final URI tokenServer1 = URI.create("https://foo1.com/bar");
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    UserCredentials credentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(httpTransportFactory)
        .setTokenServerUri(tokenServer1)
        .build();
    UserCredentials otherCredentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret("other client secret")
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(httpTransportFactory)
        .setTokenServerUri(tokenServer1)
        .build();
    assertFalse(credentials.equals(otherCredentials));
    assertFalse(otherCredentials.equals(credentials));
  }

  @Test
  public void equals_false_refreshToken() throws IOException {
    final URI tokenServer1 = URI.create("https://foo1.com/bar");
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    OAuth2Credentials credentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(httpTransportFactory)
        .setTokenServerUri(tokenServer1)
        .build();
    OAuth2Credentials otherCredentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken("otherRefreshToken")
        .setAccessToken(accessToken)
        .setHttpTransportFactory(httpTransportFactory)
        .setTokenServerUri(tokenServer1)
        .build();
    assertFalse(credentials.equals(otherCredentials));
    assertFalse(otherCredentials.equals(credentials));
  }

  @Test
  public void equals_false_accessToken() throws IOException {
    final URI tokenServer1 = URI.create("https://foo1.com/bar");
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    AccessToken otherAccessToken = new AccessToken("otherAccessToken", null);
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    UserCredentials credentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(httpTransportFactory)
        .setTokenServerUri(tokenServer1)
        .build();
    UserCredentials otherCredentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(otherAccessToken)
        .setHttpTransportFactory(httpTransportFactory)
        .setTokenServerUri(tokenServer1)
        .build();
    assertFalse(credentials.equals(otherCredentials));
    assertFalse(otherCredentials.equals(credentials));
  }

  @Test
  public void equals_false_transportFactory() throws IOException {
    final URI tokenServer1 = URI.create("https://foo1.com/bar");
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    MockTokenServerTransportFactory serverTransportFactory = new MockTokenServerTransportFactory();
    UserCredentials credentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(httpTransportFactory)
        .setTokenServerUri(tokenServer1)
        .build();
    UserCredentials otherCredentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(serverTransportFactory)
        .setTokenServerUri(tokenServer1)
        .build();
    assertFalse(credentials.equals(otherCredentials));
    assertFalse(otherCredentials.equals(credentials));
  }

  @Test
  public void equals_false_tokenServer() throws IOException {
    final URI tokenServer1 = URI.create("https://foo1.com/bar");
    final URI tokenServer2 = URI.create("https://foo2.com/bar");
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    MockHttpTransportFactory httpTransportFactory = new MockHttpTransportFactory();
    UserCredentials credentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(httpTransportFactory)
        .setTokenServerUri(tokenServer1)
        .build();
    UserCredentials otherCredentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(httpTransportFactory)
        .setTokenServerUri(tokenServer2)
        .build();
    assertFalse(credentials.equals(otherCredentials));
    assertFalse(otherCredentials.equals(credentials));
  }

  @Test
  public void toString_containsFields() throws IOException {
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    final URI tokenServer = URI.create("https://foo.com/bar");
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    UserCredentials credentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(transportFactory)
        .setTokenServerUri(tokenServer)
        .build();

    String expectedToString = String.format(
        "UserCredentials{requestMetadata=%s, temporaryAccess=%s, clientId=%s, refreshToken=%s, "
            + "tokenServerUri=%s, transportFactoryClassName=%s}",
        ImmutableMap.of(AuthHttpConstants.AUTHORIZATION,
            ImmutableList.of(OAuth2Utils.BEARER_PREFIX + accessToken.getTokenValue())),
        accessToken.toString(),
        CLIENT_ID,
        REFRESH_TOKEN,
        tokenServer,
        MockTokenServerTransportFactory.class.getName());
    assertEquals(expectedToString, credentials.toString());
  }

  @Test
  public void hashCode_equals() throws IOException {
    final URI tokenServer = URI.create("https://foo.com/bar");
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    UserCredentials credentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(transportFactory)
        .setTokenServerUri(tokenServer)
        .build();
    UserCredentials otherCredentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(transportFactory)
        .setTokenServerUri(tokenServer)
        .build();
    assertEquals(credentials.hashCode(), otherCredentials.hashCode());
  }

  @Test
  public void serialize() throws IOException, ClassNotFoundException {
    final URI tokenServer = URI.create("https://foo.com/bar");
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    AccessToken accessToken = new AccessToken(ACCESS_TOKEN, null);
    UserCredentials credentials = UserCredentials.newBuilder()
        .setClientId(CLIENT_ID)
        .setClientSecret(CLIENT_SECRET)
        .setRefreshToken(REFRESH_TOKEN)
        .setAccessToken(accessToken)
        .setHttpTransportFactory(transportFactory)
        .setTokenServerUri(tokenServer)
        .build();
    UserCredentials deserializedCredentials = serializeAndDeserialize(credentials);
    assertEquals(credentials, deserializedCredentials);
    assertEquals(credentials.hashCode(), deserializedCredentials.hashCode());
    assertEquals(credentials.toString(), deserializedCredentials.toString());
    assertSame(deserializedCredentials.clock, Clock.SYSTEM);
  }

  @Test
  public void fromStream_nullTransport_throws() throws IOException {
    InputStream stream = new ByteArrayInputStream("foo".getBytes());
    try {
      UserCredentials.fromStream(stream, null);
      fail("Should throw if HttpTransportFactory is null");
    } catch (NullPointerException expected) {
      // Expected
    }
  }

  @Test
  public void fromStream_nullStream_throws() throws IOException {
    MockHttpTransportFactory transportFactory = new MockHttpTransportFactory();
    try {
      UserCredentials.fromStream(null, transportFactory);
      fail("Should throw if InputStream is null");
    } catch (NullPointerException expected) {
      // Expected
    }
  }

  @Test
  public void fromStream_user_providesToken() throws IOException {
    MockTokenServerTransportFactory transportFactory = new MockTokenServerTransportFactory();
    transportFactory.transport.addClient(CLIENT_ID, CLIENT_SECRET);
    transportFactory.transport.addRefreshToken(REFRESH_TOKEN, ACCESS_TOKEN);
    InputStream userStream = writeUserStream(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN);

    UserCredentials credentials = UserCredentials.fromStream(userStream, transportFactory);

    assertNotNull(credentials);
    Map<String, List<String>> metadata = credentials.getRequestMetadata(CALL_URI);
    TestUtils.assertContainsBearerToken(metadata, ACCESS_TOKEN);
  }

  @Test
  public void fromStream_userNoClientId_throws() throws IOException {
    InputStream userStream = writeUserStream(null, CLIENT_SECRET, REFRESH_TOKEN);

    testFromStreamException(userStream, "client_id");
  }

  @Test
  public void fromStream_userNoClientSecret_throws() throws IOException {
    InputStream userStream = writeUserStream(CLIENT_ID, null, REFRESH_TOKEN);

    testFromStreamException(userStream, "client_secret");
  }

  @Test
  public void fromStream_userNoRefreshToken_throws() throws IOException {
    InputStream userStream = writeUserStream(CLIENT_ID, CLIENT_SECRET, null);

    testFromStreamException(userStream, "refresh_token");
  }

  @Test
  public void saveUserCredentials_saved_throws() throws IOException {
    UserCredentials userCredentials = UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .build();
    File file = File.createTempFile("GOOGLE_APPLICATION_CREDENTIALS", null, null);
    file.deleteOnExit();

    String filePath = file.getAbsolutePath();
    userCredentials.save(filePath);
  }

  @Test
  public void saveAndRestoreUserCredential_saveAndRestored_throws() throws IOException {
    UserCredentials userCredentials = UserCredentials.newBuilder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRefreshToken(REFRESH_TOKEN)
            .build();

    File file = File.createTempFile("GOOGLE_APPLICATION_CREDENTIALS", null, null);
    file.deleteOnExit();

    String filePath = file.getAbsolutePath();

    userCredentials.save(filePath);

    FileInputStream inputStream = new FileInputStream(new File(filePath));

    UserCredentials restoredCredentials = UserCredentials.fromStream(inputStream);

    assertEquals(userCredentials.getClientId(), restoredCredentials.getClientId());
    assertEquals(userCredentials.getClientSecret(), restoredCredentials.getClientSecret());
    assertEquals(userCredentials.getRefreshToken(), restoredCredentials.getRefreshToken());
  }

  static GenericJson writeUserJson(String clientId, String clientSecret, String refreshToken) {
    GenericJson json = new GenericJson();
    if (clientId != null) {
      json.put("client_id", clientId);
    }
    if (clientSecret != null) {
      json.put("client_secret", clientSecret);
    }
    if (refreshToken != null) {
      json.put("refresh_token", refreshToken);
    }
    json.put("type", GoogleCredentials.USER_FILE_TYPE);
    return json;
  }

  static InputStream writeUserStream(String clientId, String clientSecret, String refreshToken)
      throws IOException {
    GenericJson json = writeUserJson(clientId, clientSecret, refreshToken);
    return TestUtils.jsonToInputStream(json);
  }

  private static void testFromStreamException(InputStream stream, String expectedMessageContent) {
    try {
      UserCredentials.fromStream(stream);
      fail(String.format("Should throw exception with message containing '%s'",
          expectedMessageContent));
    } catch (IOException expected) {
      assertTrue(expected.getMessage().contains(expectedMessageContent));
    }
  }
}
