/*
 * Copyright 2018 Google Inc.
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

package com.google.firebase.auth.hash;

/**
 * Represents the SHA256 password hashing algorithm. Can be used as an instance of
 * {@link com.google.firebase.auth.UserImportHash} when importing users.
 */
public final class Sha256 extends RepeatableHash {

  private Sha256(Builder builder) {
    super("SHA256", 1, 8192, builder);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends RepeatableHash.Builder<Builder, Sha256> {

    private Builder() {}

    @Override
    protected Builder getInstance() {
      return this;
    }

    public Sha256 build() {
      return new Sha256(this);
    }
  }
}
