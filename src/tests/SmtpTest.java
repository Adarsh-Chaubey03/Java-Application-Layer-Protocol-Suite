package tests;

import protocols.smtp.SmtpCommand;
import protocols.smtp.SmtpClient;
import core.TcpServer;
import core.ProtocolHandler;
import utils.Logger;

import java.io.*;
import java.net.Socket;

/**
 * Test suite for the SMTP protocol implementation.
 * Tests command formatting, reply parsing, and client session with a mock server.
 */
public class SmtpTest {

    private static int passed = 0;
    private static int failed = 0;

    /**
     * Run all SMTP tests.
     */
    public static void main(String[] args) {
        Logger.section("SMTP Protocol Tests");

        testHeloCommand();
        testMailFromCommand();
        testRcptToCommand();
        testDataCommand();
        testQuitCommand();
        testReplyCodeParsing();
        testReplyMessageParsing();
        testMultiLineParsing();
        testIsSuccess();
        testMockServerSession();

        Logger.separator();
        System.out.println("SMTP Tests: " + passed + " passed, " + failed + " failed");
        Logger.separator();
    }

    /* ── Unit Tests: Command Building ── */

    private static void testHeloCommand() {
        String cmd = SmtpCommand.helo("client.example.com");
        assertEqual("HELO command", "HELO client.example.com", cmd);
    }

    private static void testMailFromCommand() {
        String cmd = SmtpCommand.mailFrom("user@example.com");
        assertEqual("MAIL FROM", "MAIL FROM:<user@example.com>", cmd);
    }

    private static void testRcptToCommand() {
        String cmd = SmtpCommand.rcptTo("recipient@example.com");
        assertEqual("RCPT TO", "RCPT TO:<recipient@example.com>", cmd);
    }

    private static void testDataCommand() {
        assertEqual("DATA command", "DATA", SmtpCommand.data());
    }

    private static void testQuitCommand() {
        assertEqual("QUIT command", "QUIT", SmtpCommand.quit());
    }

    /* ── Unit Tests: Reply Parsing ── */

    private static void testReplyCodeParsing() {
        assertEqual("Reply code 220", "220", String.valueOf(SmtpCommand.parseReplyCode("220 Ready")));
        assertEqual("Reply code 250", "250", String.valueOf(SmtpCommand.parseReplyCode("250 OK")));
        assertEqual("Reply code 354", "354", String.valueOf(SmtpCommand.parseReplyCode("354 Start")));
        assertEqual("Reply code null", "-1", String.valueOf(SmtpCommand.parseReplyCode(null)));
        assertEqual("Reply code short", "-1", String.valueOf(SmtpCommand.parseReplyCode("22")));
    }

    private static void testReplyMessageParsing() {
        assertEqual("Reply message", "service ready",
                SmtpCommand.parseReplyMessage("220 service ready"));
        assertEqual("Reply message empty", "", SmtpCommand.parseReplyMessage("250"));
    }

    private static void testMultiLineParsing() {
        assertThat("Multi-line with hyphen", SmtpCommand.isMultiLine("250-Hello"));
        assertThat("Single-line with space", !SmtpCommand.isMultiLine("250 OK"));
        assertThat("Null", !SmtpCommand.isMultiLine(null));
    }

    private static void testIsSuccess() {
        assertThat("200 is success", SmtpCommand.isSuccess(200));
        assertThat("250 is success", SmtpCommand.isSuccess(250));
        assertThat("354 is success", SmtpCommand.isSuccess(354));
        assertThat("450 is not success", !SmtpCommand.isSuccess(450));
        assertThat("550 is not success", !SmtpCommand.isSuccess(550));
    }

    /* ── Integration Test: Mock SMTP Server ── */

    private static void testMockServerSession() {
        int port = 9025;

        /* Start a mock SMTP server */
        TcpServer mockServer = new TcpServer(port, new MockSmtpHandler());
        mockServer.startInBackground();

        try {
            Thread.sleep(500);

            SmtpClient client = new SmtpClient("127.0.0.1", port);
            client.sendEmail(
                    "test@sender.com",
                    "test@recipient.com",
                    "Test Subject",
                    "Test body content"
            );

            pass("Mock SMTP session completed successfully");
        } catch (Exception e) {
            fail("Mock SMTP session failed: " + e.getMessage());
        } finally {
            mockServer.stop();
        }
    }

    /**
     * Mock SMTP server handler that simulates the SMTP command sequence.
     */
    private static class MockSmtpHandler implements ProtocolHandler {
        @Override
        public void handle(Socket clientSocket) {
            try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream()))) {

                /* Send greeting */
                writer.write("220 mock.smtp.server ESMTP Ready\r\n");
                writer.flush();

                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.debug("Mock SMTP received: " + line);

                    if (line.startsWith("HELO") || line.startsWith("EHLO")) {
                        writer.write("250 Hello\r\n");
                    } else if (line.startsWith("MAIL FROM")) {
                        writer.write("250 OK\r\n");
                    } else if (line.startsWith("RCPT TO")) {
                        writer.write("250 OK\r\n");
                    } else if (line.equals("DATA")) {
                        writer.write("354 Start input\r\n");
                        writer.flush();
                        /* Read until lone dot */
                        while ((line = reader.readLine()) != null) {
                            if (line.equals(".")) break;
                        }
                        writer.write("250 OK: message accepted\r\n");
                    } else if (line.equals("QUIT")) {
                        writer.write("221 Bye\r\n");
                        writer.flush();
                        break;
                    } else {
                        writer.write("500 Unknown command\r\n");
                    }
                    writer.flush();
                }
            } catch (Exception e) {
                Logger.error("Mock SMTP handler error: " + e.getMessage());
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
