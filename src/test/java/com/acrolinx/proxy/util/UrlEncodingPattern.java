/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import static com.github.tomakehurst.wiremock.common.Strings.normalisedLevenshteinDistance;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

final class UrlEncodingPattern extends StringValuePattern {
  static StringValuePattern equalTo(String value) {
    return new UrlEncodingPattern(value);
  }

  private static String urlDecode(String string) {
    return URLDecoder.decode(string, StandardCharsets.UTF_8);
  }

  private UrlEncodingPattern(@JsonProperty("equalTo") String expectedValue) {
    super(expectedValue);
  }

  @Override
  public MatchResult match(final String value) {
    return new MatchResult() {
      @Override
      public double getDistance() {
        return normalisedLevenshteinDistance(getValue(), urlDecode(value));
      }

      @Override
      public boolean isExactMatch() {
        return getValue().equals(urlDecode(value));
      }
    };
  }
}
