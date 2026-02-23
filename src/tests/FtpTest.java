package tests;

import protocols.ftp.FtpResponse;
import protocols.ftp.FtpClient;
import core.TcpServer;
import core.ProtocolHandler;
import utils.Logger;

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;

/**
 * Test suite for the FTP protocol implementation.
 * Tests response parsing and a mock FTP server session.
 */
public class FtpTest {

    private static int passed = 0;
    private static int failed = 0;

    /**
     * Run all FTP tests.
     */
    public static void main(String[] args) {
        Logger.section("FTP Protocol Tests");

        testResponseParsing();
        testResponseCodes();
        testMultiLineResponse();
        testIsSuccess();
        testIsComplete();
        testMockFtpSession();

        Logger.separator();
        System.out.println("FTP Tests: " + passed + " passed, " + failed + " failed");
        Logger.separator();
    }

    /* ── Unit Tests ── */

    private static void testResponseParsing() {
        FtpResponse resp = new FtpResponse("220 Welcome to FTP server");
        assertEqual("Response code", "220", String.valueOf(resp.getCode()));
        assertEqual("Response message", "Welcome to FTP server", resp.getMessage());
    }

    private static void testResponseCodes() {
        assertEqual("230 code", "230", String.valueOf(new FtpResponse("230 Login successful").getCode()));
        assertEqual("331 code", "331", String.valueOf(new FtpResponse("331 Password required").getCode()));
        assertEqual("550 code", "550", String.valueOf(new FtpResponse("550 File not found").getCode()));
        assertEqual("Null response", "-1", String.valueOf(new FtpResponse(null).getCode()));
    }

    private static void testMultiLineResponse() {
        FtpResponse multi = new FtpResponse("220-Welcome");
        FtpResponse single = new FtpResponse("220 Welcome");
        assertThat("Multi-line detected", multi.isMultiLine());
        assertThat("Single-line detected", !single.isMultiLine());
    }

    private static void testIsSuccess() {
        assertThat("220 is success", new FtpResponse("220 Ready").isSuccess());
        assertThat("150 is success", new FtpResponse("150 Opening").isSuccess());
        assertThat("230 is success", new FtpResponse("230 OK").isSuccess());
        assertThat("550 is not success", !new FtpResponse("550 Error").isSuccess());
    }

    private static void testIsComplete() {
        assertThat("226 is complete", new FtpResponse("226 Done").isComplete());
        assertThat("150 is not complete", !new FtpResponse("150 Opening").isComplete());
    }

    /* ── Integration Test: Mock FTP Server ── */

    private static void testMockFtpSession() {
        int controlPort = 9021;

        /* Start a mock FTP server */
        TcpServer mockServer = new TcpServer(controlPort, new MockFtpHandler());
        mockServer.startInBackground();

        try {
            Thread.sleep(500);

            FtpClient client = new FtpClient("127.0.0.1", controlPort);
            FtpResponse welcome = client.connect();
            assertEqual("Welcome code", "220", String.valueOf(welcome.getCode()));

            FtpResponse login = client.login("testuser", "testpass");
            assertEqual("Login code", "230", String.valueOf(login.getCode()));

            FtpResponse quit = client.quit();
            assertThat("Quit successful", quit.getCode() == 221);

            pass("Mock FTP session completed successfully");
        } catch (Exception e) {
            fail("Mock FTP session failed: " + e.getMessage());
        } finally {
            mockServer.stop();
        }
    }

    /**
     * Mock FTP server handler that simulates basic FTP commands.
     */
    private static class MockFtpHandler implements ProtocolHandler {
        @Override
        public void handle(Socket clientSocket) {
            try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream()))) {

                /* Send welcome */
                writer.write("220 Mock FTP Server Ready\r\n");
                writer.flush();

                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.debug("Mock FTP received: " + line);

                    if (line.startsWith("USER")) {
                        writer.write("331 Password required\r\n");
                    } else if (line.startsWith("PASS")) {
                        writer.write("230 Login successful\r\n");
                    } else if (line.equals("PASV")) {
                        /* Open a data port and return PASV response */
                        try {
                            ServerSocket dataServer = new ServerSocket(0);
                            int dataPort = dataServer.getLocalPort();
                            int p1 = dataPort / 256;
                            int p2 = dataPort % 256;
                            writer.write("227 Entering Passive Mode (127,0,0,1,"
                                    + p1 + "," + p2 + ")\r\n");
                            writer.flush();

                            /* Accept data connection and send listing */
                            Socket dataSocket = dataServer.accept();
                            BufferedWriter dataWriter = new BufferedWriter(
                                    new OutputStreamWriter(dataSocket.getOutputStream()));
                            dataWriter.write("-rw-r--r-- 1 user group 1024 Jan 01 00:00 test.txt\r\n");
                            dataWriter.write("drwxr-xr-x 2 user group 4096 Jan 01 00:00 documents\r\n");
                            dataWriter.flush();
                            dataWriter.close();
                            dataSocket.close();
                            dataServer.close();
                            continue;
                        } catch (Exception e) {
                            writer.write("425 Cannot open data connection\r\n");
                        }
                    } else if (line.equals("LIST")) {
                        writer.write("150 Opening data connection\r\n");
                        writer.flush();
                        Thread.sleep(200);
                        writer.write("226 Transfer complete\r\n");
                    } else if (line.equals("QUIT")) {
                        writer.write("221 Goodbye\r\n");
                        writer.flush();
                        break;
                    } else {
                        writer.write("500 Unknown command\r\n");
                    }
                    writer.flush();
                }
            } catch (Exception e) {
                Logger.error("Mock FTP handler error: " + e.getMessage());
            }
        }
    }

    /* ── Assertion helpers ── */

    private static void assertEqual(String name, String expected, String actual) {
        if (expected.equals(actual)) {
            pass(name + " = " + actual);
        } else {
            fail(name + ": expected '" + expected + "' but got '" + actual + "'");
        }
    }

    private static void assertThat(String name, boolean condition) {
        if (condition) {
            pass(name);
        } else {
            fail(name);
        }
    }

    private static void pass(String msg) {
        passed++;
        System.out.println("  ✓ PASS: " + msg);
    }

    private static void fail(String msg) {
        failed++;
        System.out.println("  ✗ FAIL: " + msg);
    }
}
