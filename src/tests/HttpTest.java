package tests;

import protocols.http.HttpRequest;
import protocols.http.HttpResponse;
import protocols.http.HttpClient;
import protocols.http.HttpServer;
import utils.Config;
import utils.Logger;

/**
 * Test suite for the HTTP protocol implementation.
 * Tests request/response parsing, serialization, and client-server integration.
 */
public class HttpTest {

    /** Test pass counter */
    private static int passed = 0;

    /** Test fail counter */
    private static int failed = 0;

    /**
     * Run all HTTP tests.
     */
    public static void main(String[] args) {
        Logger.section("HTTP Protocol Tests");

        testRequestParsing();
        testRequestSerialization();
        testResponseParsing();
        testResponseSerialization();
        testResponseAutoContentLength();
        testInvalidRequestParsing();
        testClientServerIntegration();

        Logger.separator();
        System.out.println("HTTP Tests: " + passed + " passed, " + failed + " failed");
        Logger.separator();
    }

    /* ── Unit Tests ── */

    private static void testRequestParsing() {
        String raw = "GET /index.html HTTP/1.1\r\n"
                + "Host: example.com\r\n"
                + "User-Agent: TestClient/1.0\r\n"
                + "\r\n";

        HttpRequest request = HttpRequest.parse(raw);

        assertEqual("Request.method", "GET", request.getMethod());
        assertEqual("Request.path", "/index.html", request.getPath());
        assertEqual("Request.version", "HTTP/1.1", request.getVersion());
        assertEqual("Request.Host header", "example.com", request.getHeaders().get("Host"));
        assertEqual("Request.header count", "2", String.valueOf(request.getHeaders().size()));
    }

    private static void testRequestSerialization() {
        HttpRequest request = new HttpRequest("GET", "/api/data");
        request.addHeader("Host", "api.example.com");
        request.addHeader("Accept", "application/json");

        String serialized = request.serialize();

        assertContains("Serialized request line", serialized, "GET /api/data HTTP/1.1");
        assertContains("Serialized Host header", serialized, "Host: api.example.com");
        assertContains("Serialized Accept header", serialized, "Accept: application/json");
    }

    private static void testResponseParsing() {
        String raw = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/html\r\n"
                + "Content-Length: 13\r\n"
                + "\r\n"
                + "Hello, World!";

        HttpResponse response = HttpResponse.parse(raw);

        assertEqual("Response.version", "HTTP/1.1", response.getVersion());
        assertEqual("Response.statusCode", "200", String.valueOf(response.getStatusCode()));
        assertEqual("Response.reasonPhrase", "OK", response.getReasonPhrase());
        assertEqual("Response.Content-Type", "text/html", response.getHeaders().get("Content-Type"));
        assertEqual("Response.body", "Hello, World!", response.getBody());
    }

    private static void testResponseSerialization() {
        HttpResponse response = new HttpResponse(404, "Not Found");
        response.addHeader("Content-Type", "text/plain");
        response.setBody("Page not found");

        String serialized = response.serialize();

        assertContains("Serialized status line", serialized, "HTTP/1.1 404 Not Found");
        assertContains("Serialized Content-Type", serialized, "Content-Type: text/plain");
        assertContains("Serialized body", serialized, "Page not found");
    }

    private static void testResponseAutoContentLength() {
        HttpResponse response = new HttpResponse(200, "OK");
        response.setBody("Test body");
        String serialized = response.serialize();

        assertContains("Auto Content-Length", serialized, "Content-Length: 9");
    }

    private static void testInvalidRequestParsing() {
        try {
            HttpRequest.parse("INVALID");
            fail("Invalid request should throw exception");
        } catch (IllegalArgumentException e) {
            pass("Invalid request correctly throws exception");
        }
    }

    /* ── Integration Test ── */

    private static void testClientServerIntegration() {
        int port = 9090; /* Use a test-specific port */

        /* Start the HTTP server */
        HttpServer server = new HttpServer(port);
        server.startInBackground();

        try {
            Thread.sleep(1000);

            /* Send a GET request */
            HttpClient client = new HttpClient("127.0.0.1", port);
            HttpResponse response = client.get("/");

            assertEqual("Integration.statusCode", "200", String.valueOf(response.getStatusCode()));
            assertContains("Integration.body", response.getBody(), "Java Application Layer");

            /* Test 404 */
            HttpResponse notFound = client.get("/nonexistent");
            assertEqual("Integration.404", "404", String.valueOf(notFound.getStatusCode()));

            pass("Client-Server integration test passed");
        } catch (Exception e) {
            fail("Integration test failed: " + e.getMessage());
        } finally {
            server.stop();
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

    private static void assertContains(String name, String haystack, String needle) {
        if (haystack.contains(needle)) {
            pass(name + " contains '" + needle + "'");
        } else {
            fail(name + " does not contain '" + needle + "'");
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
