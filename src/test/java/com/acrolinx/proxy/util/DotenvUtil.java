/* Copyright (c) 2023 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import io.github.cdimascio.dotenv.Dotenv;

public final class DotenvUtil {
  private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

  public static String getEnvironmentVariable(String variableKey) {
    String variableValue = dotenv.get(variableKey);

    if (variableValue == null) {
      throw new IllegalArgumentException("missing environment variable: " + variableKey);
    }

    return variableValue;
  }

  private DotenvUtil() {
    throw new IllegalStateException();
  }
}
