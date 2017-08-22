// Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8;

/**
 * Exception to signal features that are not supported until a given API level.
 */
public class ApiLevelException extends CompilationException {

  public ApiLevelException(
      int minApiLevel, String minApiLevelString, String unsupportedFeatures, String sourceString) {
    super(makeMessage(minApiLevel, minApiLevelString, unsupportedFeatures, sourceString));
    assert minApiLevel > 0;
    assert minApiLevelString != null;
    assert unsupportedFeatures != null;
  }

  private static String makeMessage(
      int minApiLevel, String minApiLevelString, String unsupportedFeatures, String sourceString) {
    String message =
        unsupportedFeatures
            + " are only supported starting with "
            + minApiLevelString
            + " (--min-api "
            + minApiLevel
            + ")";
    message = (sourceString != null) ? message + ": " + sourceString : message;
    return message;
  }
}