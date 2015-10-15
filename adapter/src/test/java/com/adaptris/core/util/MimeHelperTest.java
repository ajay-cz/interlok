/*
 * Copyright 2015 Adaptris Ltd.
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

package com.adaptris.core.util;

import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.BaseCase;
import com.adaptris.util.text.mime.MultiPartInput;

public class MimeHelperTest extends BaseCase {

  public static final String TEXT = "The quick brown fox";
  public static final String TEXT2 = "jumps over the lazy dog";

  private static final String MIME_PAYLOAD = "------=_Part_1_27366488.1056689200344\r\n" + "Content-Id: Part1\r\n" + "\r\n" + TEXT
      + "\r\n" + "------=_Part_1_27366488.1056689200344\r\n" + "Content-Id: Part2\r\n" + "\r\n" + TEXT2 + "\r\n"
      + "------=_Part_1_27366488.1056689200344--\r\n";

  private static final String MIME_HEADER = "Message-ID: 07959cb0-ffff-ffc0-019a-32e0f97a329a\r\n" + "Mime-Version: 1.0\r\n"
      + "Content-Type: multipart/mixed;\r\n" + "  boundary=\"----=_Part_1_27366488.1056689200344\"";

  public MimeHelperTest(String s) {
    super(s);
  }


  public void testCreateFromFake() throws Exception {
    MultiPartInput m = MimeHelper.create(AdaptrisMessageFactory.getDefaultInstance().newMessage(MIME_PAYLOAD));
    assertNotNull(m);
    assertEquals(2, m.size());
  }

  public void testCreateFromReal() throws Exception {
    MultiPartInput m = MimeHelper.create(AdaptrisMessageFactory.getDefaultInstance().newMessage(
        MIME_HEADER + "\r\n\r\n" + MIME_PAYLOAD));
    assertNotNull(m);
    assertEquals(2, m.size());
  }

  public void testCreateFromInvalid() throws Exception {
    try {
      MultiPartInput m = MimeHelper.create(AdaptrisMessageFactory.getDefaultInstance().newMessage("AAAAAAAA"));
      fail("Failed");
    }
    catch (Exception expected) {

    }
  }


}
