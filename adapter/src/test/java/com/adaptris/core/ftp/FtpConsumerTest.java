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

package com.adaptris.core.ftp;

import static com.adaptris.core.ftp.EmbeddedFtpServer.DEFAULT_FILENAME;
import static com.adaptris.core.ftp.EmbeddedFtpServer.DEFAULT_PASSWORD;
import static com.adaptris.core.ftp.EmbeddedFtpServer.DEFAULT_PROC_DIR_CANONICAL;
import static com.adaptris.core.ftp.EmbeddedFtpServer.DEFAULT_USERNAME;
import static com.adaptris.core.ftp.EmbeddedFtpServer.DEFAULT_WORK_DIR_CANONICAL;
import static com.adaptris.core.ftp.EmbeddedFtpServer.DEFAULT_WORK_DIR_NAME;
import static com.adaptris.core.ftp.EmbeddedFtpServer.DESTINATION_URL_OVERRIDE;
import static com.adaptris.core.ftp.EmbeddedFtpServer.PAYLOAD;
import static com.adaptris.core.ftp.EmbeddedFtpServer.SERVER_ADDRESS;
import static com.adaptris.core.ftp.EmbeddedFtpServer.SLASH;

import java.util.concurrent.TimeUnit;

import org.apache.oro.io.GlobFilenameFilter;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.ConfiguredConsumeDestination;
import com.adaptris.core.ConsumeDestination;
import com.adaptris.core.MimeEncoder;
import com.adaptris.core.QuartzCronPoller;
import com.adaptris.core.StandaloneConsumer;
import com.adaptris.core.stubs.MockMessageListener;
import com.adaptris.ftp.FtpDataMode;
import com.adaptris.util.TimeInterval;

public class FtpConsumerTest extends FtpConsumerCase {

  public FtpConsumerTest(String name) {
    super(name);
  }

  @Override
  protected FtpConnection createConnectionForExamples() {
    FtpConnection con = new FtpConnection();
    con.setDefaultUserName("default-username-if-not-specified");
    con.setDefaultPassword("default-password-if-not-specified");
    return con;
  }

  @Override
  protected String getScheme() {
    return "ftp";
  }

  @Override
  protected String createBaseFileName(Object object) {
    return super.createBaseFileName(object);
  }

  public void testFileFilterImp() throws Exception {
    FtpConsumer ftpConsumer = new FtpConsumer();
    assertNull(ftpConsumer.getFileFilterImp());
    assertEquals(GlobFilenameFilter.class.getCanonicalName(), ftpConsumer.fileFilterImp());

    ftpConsumer.setFileFilterImp("ABCDE");
    assertEquals("ABCDE", ftpConsumer.getFileFilterImp());
    assertEquals("ABCDE", ftpConsumer.fileFilterImp());
    
    ftpConsumer.setFileFilterImp(null);
    assertNull(ftpConsumer.getFileFilterImp());
    assertEquals(GlobFilenameFilter.class.getCanonicalName(), ftpConsumer.fileFilterImp());
  }

  public void testWipSuffix() throws Exception {
    FtpConsumer ftpConsumer = new FtpConsumer();
    assertNull(ftpConsumer.getWipSuffix());
    assertEquals("_wip", ftpConsumer.wipSuffix());

    ftpConsumer.setWipSuffix("ABCDE");
    assertEquals("ABCDE", ftpConsumer.getWipSuffix());
    assertEquals("ABCDE", ftpConsumer.wipSuffix());

    ftpConsumer.setWipSuffix(null);
    assertNull(ftpConsumer.getWipSuffix());
    assertEquals("_wip", ftpConsumer.wipSuffix());
  }

  public void testBasicConsume() throws Exception {
    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener();
    FakeFtpServer server = helper.createAndStart(helper.createFilesystem(count));
    StandaloneConsumer sc = null;
    try {
      FtpConsumer ftpConsumer = createForTests(listener, "testBasicConsume");
      FtpConnection consumeConnection = create(server);
      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      waitForMessages(listener, count);
      helper.assertMessages(listener.getMessages(), count);
    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }

  }

  public void testBasicConsume_NoDebug() throws Exception {
    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener();
    FakeFtpServer server = helper.createAndStart(helper.createFilesystem(count));
    StandaloneConsumer sc = null;
    try {
      FtpConsumer ftpConsumer = createForTests(listener, "testBasicConsume");
      FtpConnection consumeConnection = create(server);
      consumeConnection.setAdditionalDebug(false);
      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      waitForMessages(listener, count);
      helper.assertMessages(listener.getMessages(), count);
    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }

  }

  public void testConsumeWithOverride() throws Exception {
    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener();
    FakeFtpServer server = helper.createAndStart(helper.createFilesystem(count));
    StandaloneConsumer sc = null;
    try {
      ConfiguredConsumeDestination ccd = new ConfiguredConsumeDestination(DESTINATION_URL_OVERRIDE, null, "testConsumeWithOverride");
      FtpConsumer ftpConsumer = createForTests(listener, ccd);
      FtpConnection consumeConnection = create(server);
      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      waitForMessages(listener, count);
      helper.assertMessages(listener.getMessages(), count);
    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }
  }

  public void testConsumeWithFilter() throws Exception {
    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener();
    FileSystem filesystem = helper.createFilesystem_DirsOnly();
    for (int i = 0; i < count; i++) {
      filesystem.add(new FileEntry(DEFAULT_WORK_DIR_CANONICAL + SLASH + DEFAULT_FILENAME + i + ".txt", PAYLOAD));
    }
    FakeFtpServer server = helper.createAndStart(filesystem);
    StandaloneConsumer sc = null;
    try {
      ConfiguredConsumeDestination ccd = new ConfiguredConsumeDestination(SERVER_ADDRESS, "*.txt", "testConsumeWithFilter");
      FtpConsumer ftpConsumer = createForTests(listener, ccd);
      ftpConsumer.setFileFilterImp("org.apache.oro.io.GlobFilenameFilter");
      FtpConnection consumeConnection = create(server);
      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      waitForMessages(listener, count);
      helper.assertMessages(listener.getMessages(), count);
    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }
  }

  public void testConsumeWithQuietPeriod() throws Exception {

    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener();
    FakeFtpServer server = helper.createAndStart(helper.createFilesystem(count));
    StandaloneConsumer sc = null;
    try {
      FtpConsumer ftpConsumer = createForTests(listener, "testConsumeWithQuietPeriod");
      ftpConsumer.setQuietInterval(new TimeInterval(1L, TimeUnit.SECONDS));
      FtpConnection consumeConnection = create(server);
      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      waitForMessages(listener, count);
      helper.assertMessages(listener.getMessages(), count);
    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }
  }

  public void testConsumeWithNonMatchingFilter() throws Exception {
    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener();
    FileSystem filesystem = helper.createFilesystem_DirsOnly();
    for (int i = 0; i < count; i++) {
      filesystem.add(new FileEntry(DEFAULT_WORK_DIR_CANONICAL + SLASH + DEFAULT_FILENAME + i + ".txt", PAYLOAD));
    }
    FakeFtpServer server = helper.createAndStart(filesystem);
    StandaloneConsumer sc = null;
    try {
      ConfiguredConsumeDestination ccd = new ConfiguredConsumeDestination(SERVER_ADDRESS, "*.xml",
          "testConsumeWithNonMatchingFilter");
      FtpConsumer ftpConsumer = createForTests(listener, ccd);
      ftpConsumer.setFileFilterImp("org.apache.oro.io.GlobFilenameFilter");
      FtpConnection consumeConnection = create(server);
      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      // Short sleep to make sure we trip the poll
      Thread.sleep(1500);

      helper.assertMessages(listener.getMessages(), 0);
      assertEquals(count, filesystem.listFiles(DEFAULT_WORK_DIR_CANONICAL).size());
    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }
  }

  public void testActiveModeConsume() throws Exception {

    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener(100);
    FakeFtpServer server = helper.createAndStart(helper.createFilesystem(count));
    StandaloneConsumer sc = null;
    try {
      FtpConsumer ftpConsumer = createForTests(listener, "testActiveModeConsume");
      FtpConnection consumeConnection = create(server);
      consumeConnection.setFtpDataMode(FtpDataMode.ACTIVE);
      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      waitForMessages(listener, count);
      helper.assertMessages(listener.getMessages(), count);
    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }
  }

  public void testPassiveModeConsume() throws Exception {

    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener(100);
    FakeFtpServer server = helper.createAndStart(helper.createFilesystem(count));
    StandaloneConsumer sc = null;
    try {
      FtpConsumer ftpConsumer = createForTests(listener, "testPassiveModeConsume");
      FtpConnection consumeConnection = create(server);
      consumeConnection.setFtpDataMode(FtpDataMode.PASSIVE);
      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      waitForMessages(listener, count);
      helper.assertMessages(listener.getMessages(), count);
    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }
  }

  public void testConsume_ForceRelativePath() throws Exception {
    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener(100);
    FakeFtpServer server = helper.createAndStart(helper.createFilesystem(count));
    StandaloneConsumer sc = null;
    try {
      FtpConsumer ftpConsumer = createForTests(listener, "testConsume_ForceRelativePath");
      ftpConsumer.setWorkDirectory(SLASH + DEFAULT_WORK_DIR_NAME);
      FtpConnection consumeConnection = create(server);
      consumeConnection.setForceRelativePath(Boolean.TRUE);
      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      waitForMessages(listener, count);
      helper.assertMessages(listener.getMessages(), count);
    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }
  }

  public void testConsumeWithQuietPeriodAndTimezone() throws Exception {
    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener(100);
    FileSystem filesystem = helper.createFilesystem_DirsOnly();
    for (int i = 0; i < count; i++) {
      filesystem.add(new FileEntry(DEFAULT_WORK_DIR_CANONICAL + SLASH + DEFAULT_FILENAME + i + ".txt", PAYLOAD));
    }
    FakeFtpServer server = helper.createAndStart(filesystem);
    StandaloneConsumer sc = null;
    try {
      FtpConsumer ftpConsumer = createForTests(listener, "testConsumeWithQuietPeriodAndTimezone");
      ftpConsumer.setQuietInterval(new TimeInterval(3L, TimeUnit.SECONDS));
      FtpConnection consumeConnection = create(server);
      consumeConnection.setServerTimezone("America/Los_Angeles");

      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      // Short sleep to make sure we trip the poll
      Thread.sleep(1500);

      helper.assertMessages(listener.getMessages(), 0);
      assertEquals(count, filesystem.listFiles(DEFAULT_WORK_DIR_CANONICAL).size());

    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }
  }

  public void testConsume_WithProcDirectory() throws Exception {

    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener(100);
    FileSystem filesystem = helper.createFilesystem_DirsOnly();
    for (int i = 0; i < count; i++) {
      filesystem.add(new FileEntry(DEFAULT_WORK_DIR_CANONICAL + SLASH + DEFAULT_FILENAME + i + ".txt", PAYLOAD));
    }
    FakeFtpServer server = helper.createAndStart(filesystem);
    StandaloneConsumer sc = null;
    try {
      FtpConsumer ftpConsumer = createForTests(listener, "testConsume_WithProcDirectory");
      ftpConsumer.setProcDirectory(DEFAULT_PROC_DIR_CANONICAL);
      FtpConnection consumeConnection = create(server);
      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      waitForMessages(listener, count);
      Thread.sleep(500);
      helper.assertMessages(listener.getMessages(), count);
      // assertEquals(count, filesystem.listFiles(DEFAULT_PROC_DIR_CANONICAL).size());

    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }
  }

  public void testConsume_WithProcDirectory_FileAlreadyExists() throws Exception {
    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener(100);
    FileSystem filesystem = helper.createFilesystem_DirsOnly();
    for (int i = 0; i < count; i++) {
      filesystem.add(new FileEntry(DEFAULT_WORK_DIR_CANONICAL + SLASH + DEFAULT_FILENAME + i + ".txt", PAYLOAD));
      filesystem.add(new FileEntry(DEFAULT_PROC_DIR_CANONICAL + SLASH + DEFAULT_FILENAME + i + ".txt", PAYLOAD));
    }
    FakeFtpServer server = helper.createAndStart(filesystem);
    StandaloneConsumer sc = null;
    try {
      FtpConsumer ftpConsumer = createForTests(listener, "testConsume_WithProcDirectory_FileAlreadyExists");
      ftpConsumer.setProcDirectory(DEFAULT_PROC_DIR_CANONICAL);
      FtpConnection consumeConnection = create(server);
      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      waitForMessages(listener, count);
      Thread.sleep(500);
      // Because the files already exist in the PROC dir, we expect file1.txt and file1.txt.timestamp;
      // assertEquals(count * 2, filesystem.listFiles(DEFAULT_PROC_DIR_CANONICAL).size());
    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }
  }

  public void testConsumeWithEncoder() throws Exception {
    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener(100);
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance().newMessage(PAYLOAD);
    byte[] bytes = new MimeEncoder().encode(msg);
    FileSystem filesystem = helper.createFilesystem_DirsOnly();
    for (int i = 0; i < count; i++) {
      FileEntry entry = new FileEntry(DEFAULT_WORK_DIR_CANONICAL + SLASH + DEFAULT_FILENAME + i + ".txt");
      entry.setContents(bytes);
      filesystem.add(entry);
    }
    FakeFtpServer server = helper.createAndStart(filesystem);
    StandaloneConsumer sc = null;
    try {
      FtpConsumer ftpConsumer = createForTests(listener, "testConsumeWithEncoder");
      ftpConsumer.setEncoder(new MimeEncoder());
      FtpConnection consumeConnection = create(server);
      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      waitForMessages(listener, count);
      helper.assertMessages(listener.getMessages(), count);
    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }
  }

  public void testConsume_IgnoresWipFiles() throws Exception {

    int count = 1;
    EmbeddedFtpServer helper = new EmbeddedFtpServer();
    MockMessageListener listener = new MockMessageListener(100);
    FtpConsumer ftpConsumer = createForTests(listener, "testConsume_IgnoresWipFiles");
    FileSystem filesystem = helper.createFilesystem_DirsOnly();
    for (int i = 0; i < count; i++) {
      filesystem.add(new FileEntry(DEFAULT_WORK_DIR_CANONICAL + SLASH + DEFAULT_FILENAME + i + ".txt", PAYLOAD));
    }
    // Now create some files that have a _wip extension.
    filesystem.add(new FileEntry(DEFAULT_WORK_DIR_CANONICAL + SLASH + "shouldBeIgnored.txt" + ftpConsumer.wipSuffix(), PAYLOAD));
    FakeFtpServer server = helper.createAndStart(filesystem);
    StandaloneConsumer sc = null;
    try {
      FtpConnection consumeConnection = create(server);
      sc = new StandaloneConsumer(consumeConnection, ftpConsumer);
      start(sc);
      waitForMessages(listener, count);
      helper.assertMessages(listener.getMessages(), count);
      Thread.sleep(2000); // allow the consumer to consume the single message, should be 1 file left - the .wip file.
      assertTrue(filesystem.listFiles(DEFAULT_WORK_DIR_CANONICAL).size() > 0);
    }
    catch (Exception e) {
      throw e;
    }
    finally {
      stop(sc);
      server.stop();
    }
  }

  private FtpConnection create(FakeFtpServer server) {
    FtpConnection consumeConnection = new FtpConnection();
    consumeConnection.setDefaultControlPort(server.getServerControlPort());
    consumeConnection.setDefaultPassword(DEFAULT_PASSWORD);
    consumeConnection.setDefaultUserName(DEFAULT_USERNAME);
    consumeConnection.setCacheConnection(true);
    consumeConnection.setAdditionalDebug(true);
    return consumeConnection;
  }

  private FtpConsumer createForTests(MockMessageListener listener, String threadName) {
    return createForTests(listener, new ConfiguredConsumeDestination(SERVER_ADDRESS, null, threadName));
  }

  private FtpConsumer createForTests(MockMessageListener listener, ConsumeDestination dest) {
    FtpConsumer ftpConsumer = new FtpConsumer();
    if (dest.getDestination().equals(SERVER_ADDRESS)) {
      ftpConsumer.setWorkDirectory(DEFAULT_WORK_DIR_CANONICAL);
    }
    else {
      ftpConsumer.setWorkDirectory(SLASH + DEFAULT_WORK_DIR_NAME);
    }
    ftpConsumer.setDestination(dest);
    ftpConsumer.registerAdaptrisMessageListener(listener);
    ftpConsumer.setPoller(new QuartzCronPoller("*/1 * * * * ?", dest.getDeliveryThreadName()));
    return ftpConsumer;
  }

}
