# Application Layer Protocol Stack — Java

A modular, production-quality Java project that implements core **Application Layer protocols** from scratch using only standard Java libraries (`java.net`, `java.io`, `java.nio`). No third-party libraries used.

## Protocols Implemented

| Protocol | Type | Description |
|----------|------|-------------|
| **HTTP** | Client + Server | Manual request/response parsing, GET support, static HTML serving |
| **DNS** | Client | Binary query/response per RFC 1035, A/CNAME/AAAA/MX records |
| **SMTP** | Client | Full HELO→MAIL FROM→RCPT TO→DATA→QUIT sequence per RFC 5321 |
| **FTP** | Client | Control connection, USER/PASS auth, PASV mode, LIST command |
| **DHCP** | Simulated | Full DISCOVER→OFFER→REQUEST→ACK (DORA) exchange on localhost |

## Project Structure

```
src/
├── core/                         # Networking abstractions
│   ├── ProtocolHandler.java      # Strategy interface for protocol servers
│   ├── SocketClient.java         # TCP client wrapper (connect/send/recv/timeout)
│   ├── UdpClient.java            # UDP datagram wrapper
│   └── TcpServer.java            # Multi-threaded TCP server base
├── protocols/
│   ├── http/
│   │   ├── HttpRequest.java      # HTTP request parser/builder (RFC 2616)
│   │   ├── HttpResponse.java     # HTTP response parser/builder
│   │   ├── HttpClient.java       # Manual GET client via raw sockets
│   │   └── HttpServer.java       # Simple HTTP server with routing
│   ├── dns/
│   │   ├── DnsPacket.java        # Binary DNS packet builder/decoder (RFC 1035)
│   │   └── DnsClient.java        # UDP DNS resolver client
│   ├── smtp/
│   │   ├── SmtpCommand.java      # SMTP command builder/reply parser (RFC 5321)
│   │   └── SmtpClient.java       # Full SMTP session handler
│   ├── ftp/
│   │   ├── FtpResponse.java      # FTP reply code parser (RFC 959)
│   │   └── FtpClient.java        # FTP client with PASV and LIST
│   └── dhcp/
│       ├── DhcpPacket.java       # DHCP packet builder/decoder (RFC 2131)
│       ├── DhcpSimServer.java    # Simulated DHCP server (localhost)
│       └── DhcpSimClient.java    # Simulated DHCP client (DORA sequence)
├── utils/
│   ├── Logger.java               # Level-based logging (INFO/DEBUG/ERROR)
│   ├── Config.java               # Runtime configuration holder
│   └── ByteUtils.java            # Hex dump, byte↔int, IP conversions
├── tests/
│   ├── HttpTest.java             # HTTP unit + integration tests
│   ├── DnsTest.java              # DNS packet + live resolution tests
│   ├── SmtpTest.java             # SMTP commands + mock server tests
│   ├── FtpTest.java              # FTP response + mock server tests
│   └── DhcpTest.java             # DHCP packet + simulated DORA tests
└── Main.java                     # CLI entry point (interactive menu)
```

## Architecture

```
┌─────────────────────────────────────────────────┐
│                   Main.java (CLI)               │
├─────────────────────────────────────────────────┤
│              Protocol Layer                     │
│  ┌──────┐ ┌─────┐ ┌──────┐ ┌─────┐ ┌──────┐     │
│  │ HTTP │ │ DNS │ │ SMTP │ │ FTP │ │ DHCP │     │
│  └──┬───┘ └──┬──┘ └──┬───┘ └──┬──┘ └──┬───┘     │
├─────┼────────┼───────┼────────┼───────┼──────── ┤
│     │   Core Networking Layer │       │         │
│  ┌──┴────────┴───┐  ┌────────┴───────┴──┐       │
│  │  SocketClient │  │    UdpClient      │       │
│  │  TcpServer    │  │                   │       │
│  └───────────────┘  └───────────────────┘       │
├─────────────────────────────────────────────────┤
│              Utility Layer                      │
│  ┌────────┐  ┌────────┐  ┌───────────┐          │
│  │ Logger │  │ Config │  │ ByteUtils │          │
│  └────────┘  └────────┘  └───────────┘          │
└─────────────────────────────────────────────────┘
```

## Design Decisions

- **Layered Architecture**: Core networking is fully separated from protocol logic, enabling reuse
- **Strategy Pattern**: `ProtocolHandler` interface decouples server infrastructure from protocol handling
- **AutoCloseable**: Socket wrappers implement `AutoCloseable` for try-with-resources safety
- **Binary Protocol Support**: `ByteUtils` provides endian-aware read/write for DNS/DHCP wire formats
- **Configurable**: All ports, timeouts, and server addresses are runtime-configurable via `Config`
- **No Dependencies**: Everything uses only `java.net`, `java.io`, `java.nio`, and `java.time`

## Compilation

```bash
# Compile all source files
javac -d out src/Main.java src/core/*.java src/protocols/http/*.java src/protocols/dns/*.java src/protocols/smtp/*.java src/protocols/ftp/*.java src/protocols/dhcp/*.java src/utils/*.java src/tests/*.java
```

## Running the Application

```bash
# Interactive menu
java -cp out Main

# Direct protocol selection
java -cp out Main --protocol http
java -cp out Main --protocol dns
java -cp out Main --protocol dhcp

# Verbose (debug) mode
java -cp out Main --verbose

# Custom settings
java -cp out Main --verbose --dns-server 1.1.1.1 --http-port 9090 --timeout 10000
```

## Running Tests

```bash
# Run all test suites
java -cp out tests.HttpTest
java -cp out tests.DnsTest
java -cp out tests.SmtpTest
java -cp out tests.FtpTest
java -cp out tests.DhcpTest
```

## Sample Runs & Expected Output

### HTTP Demo (option 1)
```
══════════════════════════════════════════════════════════════════════
  HTTP Protocol Demo
══════════════════════════════════════════════════════════════════════
HTTP server started on port 8080
══════════════════════════════════════════════════════════════════════
  HTTP Response
══════════════════════════════════════════════════════════════════════
Status : 200 OK
Headers:
  Content-Type: text/html; charset=UTF-8
  Server: JavaProtocolStack/1.0
Body   :
<!DOCTYPE html>
<html><head><title>Java Protocol Stack</title></head>
<body>
<h1>Welcome to the Java Application Layer Protocol Stack</h1>
</body></html>
```

### DNS Demo (option 2, domain: example.com)
```
══════════════════════════════════════════════════════════════════════
  DNS Results for example.com
══════════════════════════════════════════════════════════════════════
NAME                           TYPE   CLASS TTL        DATA
--------------------------------------------------------------------------------
example.com                    A      IN    TTL=86400  93.184.216.34
```

### DHCP Demo (option 5)
```
══════════════════════════════════════════════════════════════════════
  DHCP DORA Sequence (Simulated)
══════════════════════════════════════════════════════════════════════
  → DISCOVER sent (broadcast, looking for any DHCP server)
  ← OFFER received: IP=192.168.1.100 from server=192.168.1.1
    Subnet Mask : 255.255.255.0
    Gateway     : 192.168.1.1
    DNS Server  : 8.8.8.8
    Lease Time  : 86400 seconds
  → REQUEST sent (requesting IP=192.168.1.100)
  ← ACK received: IP=192.168.1.100 confirmed!
    Lease Time  : 86400 seconds
══════════════════════════════════════════════════════════════════════
  DHCP DORA sequence completed successfully!
  Assigned IP: 192.168.1.100
  MAC Address: AA:BB:CC:DD:EE:FF
```

### SMTP Demo (option 3)
```
══════════════════════════════════════════════════════════════════════
  SMTP Session: smtp.example.com:587
══════════════════════════════════════════════════════════════════════
[INFO] Connecting to SMTP server smtp.example.com:587
[INFO] SMTP Greeting: 220 smtp.example.com ESMTP ready
[INFO] HELO → 250 Hello client.local
[INFO] MAIL FROM → 250 OK
[INFO] RCPT TO → 250 OK
[INFO] Server ready for data: 354 Start mail input
[INFO] Message accepted: 250 OK: queued
[INFO] QUIT → 221 Bye
[INFO] Email sent successfully!
```

### FTP Demo (option 4)
```
══════════════════════════════════════════════════════════════════════
  FTP Session: ftp.dlptest.com:21
══════════════════════════════════════════════════════════════════════
[INFO] Connecting to FTP server ftp.dlptest.com:21
[INFO] FTP Welcome: 220 Welcome to the DLP Test FTP Server
[INFO] Logging in as: dlpuser
[INFO] Logged in successfully
[INFO] PASV: data connection at 35.192.130.35:47821
[INFO] Listing directory...
══════════════════════════════════════════════════════════════════════
  Directory Listing
══════════════════════════════════════════════════════════════════════
-rw-r--r--    1 1001     1001        1024 Jan 01 00:00 testfile.txt
drwxr-xr-x    2 1001     1001        4096 Jan 01 00:00 uploads
[INFO] FTP session closed
```

## Prerequisites

- **Java JDK 11+** (uses `java.time` and modern `java.net` features)
- **Internet access** for DNS resolution and external SMTP/FTP servers
- No build tools, frameworks, or third-party libraries required

## CLI Options

| Flag | Description | Example |
|------|-------------|---------|
| `--protocol <name>` | Run a protocol demo directly (skip menu) | `--protocol dns` |
| `--verbose` / `-v` | Enable debug-level logging | `--verbose` |
| `--dns-server <ip>` | Override DNS server (default: `8.8.8.8`) | `--dns-server 1.1.1.1` |
| `--http-port <port>` | Set HTTP server port (default: `8080`) | `--http-port 9090` |
| `--timeout <ms>` | Set connect/read timeout in milliseconds | `--timeout 10000` |

## Notes

- **SMTP/FTP**: These require real external servers. The tests use built-in mock servers for validation
- **DHCP**: Fully simulated on localhost (no real broadcast). Uses non-standard high ports
- **DNS**: Requires internet access to reach `8.8.8.8`. Change with `--dns-server`
- **Tests**: Use custom test harness (no JUnit dependency). Each test file is standalone

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 11+ |
| TCP Networking | `java.net.Socket`, `java.net.ServerSocket` |
| UDP Networking | `java.net.DatagramSocket`, `java.net.DatagramPacket` |
| Binary Protocols | `java.nio.ByteBuffer` |
| Concurrency | `java.lang.Thread` |
| Date/Time | `java.time.ZonedDateTime` |
| Build | `javac` (no Maven/Gradle) |

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-protocol`)
3. Commit changes (`git commit -m 'Add SNMP protocol support'`)
4. Push to branch (`git push origin feature/new-protocol`)
5. Open a Pull Request

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
