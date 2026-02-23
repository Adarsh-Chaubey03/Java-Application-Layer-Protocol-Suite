# Application Layer Protocol Stack — Academic Theory Document

> **Subject**: Computer Networks — Application Layer Protocol Implementation  
> **Language**: Java (JDK 11+)  
> **Libraries**: `java.net`, `java.io`, `java.nio`, `java.time` (no third-party dependencies)

---

## A. Project Overview

### Objective

To design and implement a modular Java application that demonstrates the inner workings of five core Application Layer protocols — **HTTP, DNS, SMTP, FTP, and DHCP** — using only standard Java socket APIs. The project constructs, parses, serializes, and transmits raw protocol messages over TCP and UDP, providing hands-on understanding of how these protocols operate at the byte level.

### Problem Statement

Most applications use high-level libraries that abstract away protocol internals. Developers rarely interact with raw request–response cycles, binary packet structures, or state-machine sequences that protocols depend on. This project bridges that gap by implementing each protocol from scratch, demonstrating message construction, serialization, parsing, and error handling as defined by their respective RFCs.

### Scope

| In Scope | Out of Scope |
|----------|-------------|
| HTTP/1.1 GET request/response cycle | HTTPS/TLS, POST/PUT/DELETE methods |
| DNS A/CNAME/AAAA/MX record queries via UDP | DNS zone transfers, DNSSEC |
| SMTP full email send session (HELO→QUIT) | ESMTP extensions (STARTTLS, AUTH) |
| FTP control connection, PASV mode, LIST | FTP data upload/download (STOR/RETR) |
| DHCP DORA sequence (simulated on localhost) | Real broadcast on port 67/68, relay agents |

### Constraints

- **Zero third-party dependencies** — Only `java.net`, `java.io`, `java.nio`, `java.time` are used
- **No build tools** — Compilation via `javac`, no Maven/Gradle
- **DHCP runs on non-standard ports** (6700/6800) to avoid privileged port restrictions
- **SMTP/FTP demos require real external servers**; tests use built-in mock servers
- **DNS requires internet access** to reach the configured resolver (default: `8.8.8.8`)

---

## B. Application Layer Protocol Theory

---

# 1. HTTP (HyperText Transfer Protocol)

## Concept

HTTP is a stateless, text-based, request–response protocol used for transferring hypertext documents on the World Wide Web. It operates at the Application Layer (Layer 7 of OSI) and relies on TCP at the Transport Layer. Defined primarily by **RFC 2616** (HTTP/1.1), it follows a client–server communication model where the client sends a request and the server returns a response.

## Architecture

```
Client (Browser/App)              Server (Web Server)
       |                                |
       |---- TCP 3-way handshake ------>|
       |                                |
       |---- HTTP GET /index.html ----->|
       |                                |
       |<--- HTTP 200 OK + HTML --------|
       |                                |
       |---- Connection: close -------->|
       |                                |
       |---- TCP FIN ------------------>|
```

**Communication Model**: Client–Server, Request–Response  
**Transport**: TCP (port 80 by default; configurable in this project via `Config.getHttpServerPort()`)

## Message Format

### HTTP Request (RFC 2616 §5)

```
Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
Headers        = *(Header-Name: Header-Value CRLF)
                 CRLF
Body           = (optional, empty for GET)
```

**Example** (as constructed by `HttpRequest.serialize()`):
```
GET / HTTP/1.1\r\n
Host: 127.0.0.1:8080\r\n
User-Agent: JavaProtocolStack/1.0\r\n
Accept: */*\r\n
Connection: close\r\n
\r\n
```

### HTTP Response (RFC 2616 §6)

```
Status-Line    = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
Headers        = *(Header-Name: Header-Value CRLF)
                 CRLF
Body           = (the resource content)
```

**Example** (as constructed by `HttpResponse.serialize()`):
```
HTTP/1.1 200 OK\r\n
Content-Type: text/html; charset=UTF-8\r\n
Server: JavaProtocolStack/1.0\r\n
Date: Mon, 24 Feb 2026 12:00:00 IST\r\n
Connection: close\r\n
Content-Length: 245\r\n
\r\n
<!DOCTYPE html>...
```

### Key Fields

| Field | Description |
|-------|-------------|
| Method | `GET`, `POST`, `PUT`, `DELETE`, etc. (project supports `GET` only) |
| Request-URI | Path of the requested resource (e.g., `/`, `/status`) |
| HTTP-Version | Protocol version string (`HTTP/1.1`) |
| Status-Code | 3-digit numeric result code (`200`, `404`, `405`) |
| Headers | Key-value metadata (`Content-Type`, `Host`, `Connection`) |
| Body | Payload — HTML, JSON, or plain text |

## Workflow

### Server State Machine (as implemented in `HttpServer.handle()`)

```
LISTEN → ACCEPT CONNECTION → READ REQUEST → PARSE (HttpRequest.parse())
  → ROUTE (processRequest())
    → "/" or "/index.html" → 200 + Welcome HTML
    → "/status"           → 200 + JSON health check
    → other               → 404 Not Found
    → non-GET method      → 405 Method Not Allowed
  → SERIALIZE RESPONSE (HttpResponse.serialize())
  → SEND → CLOSE CONNECTION
```

### Client Flow (as implemented in `HttpClient.get()`)

```
CREATE SocketClient → CONNECT to host:port
  → BUILD HttpRequest (method, path, headers)
  → SERIALIZE → SEND raw bytes
  → RECEIVE all response data
  → PARSE into HttpResponse
  → RETURN parsed response
  → CLOSE (AutoCloseable)
```

## Code Mapping

| Concept | Source File | Key Method |
|---------|-----------|------------|
| Request model | `HttpRequest.java` | `parse()`, `serialize()`, `addHeader()` |
| Response model | `HttpResponse.java` | `parse()`, `serialize()` (auto Content-Length) |
| Client operations | `HttpClient.java` | `get()`, `getAndPrint()` |
| Server operations | `HttpServer.java` | `handle()`, `processRequest()`, `getHttpDate()` |
| Server infrastructure | `TcpServer.java` | `start()`, `startInBackground()`, `stop()` |
| Strategy interface | `ProtocolHandler.java` | `handle(Socket)` |

### Design Patterns Used

- **Strategy Pattern**: `HttpServer` implements `ProtocolHandler`; `TcpServer` delegates connection handling
- **Builder Pattern**: `HttpRequest` and `HttpResponse` support programmatic construction via `addHeader()`, `setBody()`
- **AutoCloseable**: `SocketClient` used with try-with-resources in `HttpClient.get()`

## Advantages

- Simple, human-readable text format — easy to debug with packet capture tools
- Stateless — each request is independent, enabling horizontal scaling
- Extensible via headers — content negotiation, caching, authentication can be added

## Limitations

- **No HTTPS/TLS** — data is transmitted in plaintext
- **Only GET supported** — POST, PUT, DELETE not implemented
- **No persistent connections (HTTP keep-alive)** — `Connection: close` is always set
- **No chunked transfer encoding** — response is sent as a single payload
- **Single-threaded per connection** — each client gets a daemon thread but no thread pooling

## Security Considerations

- Plaintext transmission — vulnerable to eavesdropping and man-in-the-middle attacks
- No input sanitization on request paths — potential path traversal if extended to file serving
- No authentication/authorization mechanism implemented

## Comparison with Alternatives

| Feature | HTTP/1.1 (this project) | HTTP/2 | HTTP/3 |
|---------|------------------------|--------|--------|
| Transport | TCP | TCP | QUIC (UDP) |
| Multiplexing | No | Yes | Yes |
| Header compression | No | HPACK | QPACK |
| Format | Text | Binary frames | Binary frames |

## FAQ

**Q1: Why does HTTP use TCP instead of UDP?**  
A: HTTP requires reliable, ordered delivery of data. TCP provides this through acknowledgments, retransmissions, and sequencing. A partial or corrupted web page would be unusable.

**Q2: What is the role of the `Host` header in HTTP/1.1?**  
A: It enables virtual hosting — multiple websites can share a single IP address. The server uses the `Host` header to route the request to the correct website. In the code, `HttpClient` sets this via `request.addHeader("Host", host + ":" + port)`.

**Q3: How does the server in this project handle concurrent connections?**  
A: `TcpServer.start()` spawns a new daemon thread for each accepted connection via `new Thread(() -> handler.handle(clientSocket))`. This is a thread-per-connection model.

**Q4: What happens if a client sends a POST request?**  
A: `HttpServer.processRequest()` checks `request.getMethod()` and returns a `405 Method Not Allowed` response with the body "Only GET is supported."

**Q5: How is Content-Length determined automatically?**  
A: In `HttpResponse.serialize()`, if the body is non-empty and the `Content-Length` header is absent, it auto-calculates via `body.getBytes().length` and injects the header.

---

# 2. DNS (Domain Name System)

## Concept

DNS is a hierarchical, distributed naming system that translates human-readable domain names (e.g., `example.com`) into IP addresses (e.g., `93.184.216.34`). It is defined by **RFC 1035** and operates primarily over UDP on port 53 for standard queries.

## Architecture

```
Client Application
       |
       | (domain query)
       v
  DNS Resolver (Stub) ──UDP:53──> Recursive Resolver (8.8.8.8)
                                        |
                               ┌────────┼────────┐
                               v        v        v
                          Root NS   TLD NS   Authoritative NS
                          (.)       (.com)    (example.com)
```

**Communication Model**: Client–Server (query–response via UDP datagrams)  
**Transport**: UDP (port 53). TCP used only for zone transfers or responses >512 bytes.

## Message Format (RFC 1035 §4)

```
+---------------------+
|       Header        |  12 bytes (fixed)
+---------------------+
|      Question       |  variable (domain + type + class)
+---------------------+
|       Answer        |  variable (resource records)
+---------------------+
|      Authority      |  variable
+---------------------+
|     Additional      |  variable
+---------------------+
```

### Header Structure (12 bytes)

| Field | Size | Description |
|-------|------|-------------|
| ID | 16 bits | Transaction identifier (random, matched in response) |
| QR | 1 bit | 0 = query, 1 = response |
| Opcode | 4 bits | 0 = standard query |
| AA | 1 bit | Authoritative answer flag |
| TC | 1 bit | Truncation flag |
| RD | 1 bit | Recursion desired (set to 1 in `buildQuery()`) |
| RA | 1 bit | Recursion available (set by server) |
| Z | 3 bits | Reserved (0) |
| RCODE | 4 bits | Response code (0 = no error) |
| QDCOUNT | 16 bits | Number of questions (1 in this implementation) |
| ANCOUNT | 16 bits | Number of answer records |
| NSCOUNT | 16 bits | Number of authority records |
| ARCOUNT | 16 bits | Number of additional records |

### Question Section

```
QNAME  = length-prefixed labels (e.g., 7example3com0)
QTYPE  = 16-bit record type (1=A, 5=CNAME, 15=MX, 28=AAAA)
QCLASS = 16-bit class (1=IN for Internet)
```

**Domain Encoding** (implemented in `DnsPacket.encodeDomainName()`):
```
"example.com" → [7] e x a m p l e [3] c o m [0]
```

### Answer Section (Resource Record)

| Field | Size | Description |
|-------|------|-------------|
| NAME | variable | Domain name (may use compression pointers) |
| TYPE | 16 bits | Record type |
| CLASS | 16 bits | Record class |
| TTL | 32 bits | Time-to-live in seconds |
| RDLENGTH | 16 bits | Length of RDATA |
| RDATA | variable | Type-specific data (IP for A, domain for CNAME) |

### Compression Pointers

When the two high-order bits of a label length byte are `11` (0xC0), the remaining 14 bits form a pointer to a previous occurrence of the name in the packet. This is handled in `DnsPacket.decodeNameFull()`:

```java
if ((len & 0xC0) == 0xC0) {
    int pointer = ((len & 0x3F) << 8) | (data[currentOffset + 1] & 0xFF);
    currentOffset = pointer;
    jumped = true;
}
```

## Workflow

### Client Flow (`DnsClient.resolve()`)

```
BUILD DnsPacket(domain, TYPE_A)
  → buildQuery() → serialize header (12 bytes) + question section
  → SEND via UdpClient to 8.8.8.8:53
  → RECEIVE DatagramPacket (max 512 bytes)
  → DnsPacket.decodeResponse() → parse header, skip questions, parse answers
  → RETURN list of DnsRecord objects
```

### Packet Lifecycle

```
[Application]  resolve("example.com")
       |
[DnsPacket]    buildQuery() → byte[29]
       |         Header: ID=random, flags=0x0100 (RD=1), QD=1
       |         Question: 7example3com0, TYPE=1, CLASS=1
       |
[UdpClient]    send(bytes, "8.8.8.8", 53)
       |
[Network]      UDP datagram → DNS resolver → recursive lookup
       |
[UdpClient]    receive(512) → DatagramPacket
       |
[DnsPacket]    decodeResponse() → parse header, answers
       |         Answer: name="example.com", TYPE=A, TTL=86400, data="93.184.216.34"
       |
[Application]  display results
```

## Code Mapping

| Concept | Source File | Key Method |
|---------|-----------|------------|
| Packet construction | `DnsPacket.java` | `buildQuery()`, `encodeDomainName()` |
| Response decoding | `DnsPacket.java` | `decodeResponse()`, `decodeNameFull()` |
| Compression handling | `DnsPacket.java` | `decodeNameFull()` (0xC0 pointer logic) |
| IPv6 address decoding | `DnsPacket.java` | `decodeIpv6()` |
| Record type utilities | `DnsPacket.java` | `typeToString()`, constants `TYPE_A` etc. |
| Client operations | `DnsClient.java` | `resolve()`, `resolveAndPrint()` |
| UDP transport | `UdpClient.java` | `send()`, `receive()` |
| Byte operations | `ByteUtils.java` | `readUint16()`, `readUint32()`, `bytesToIp()` |

## Advantages

- Fast resolution — single UDP round-trip for most queries
- Hierarchical caching reduces load on root/TLD servers
- Supports multiple record types (A, AAAA, CNAME, MX, NS)

## Limitations

- **UDP 512-byte limit** — responses exceeding this require TCP (not implemented)
- **No caching** — each call makes a fresh network request
- **No iterative resolution** — relies on the recursive resolver (8.8.8.8)
- **No DNSSEC validation** — responses are not cryptographically verified

## Security Considerations

- DNS queries are unencrypted — vulnerable to eavesdropping
- Susceptible to DNS spoofing/cache poisoning (no DNSSEC)
- Transaction ID is randomly generated to mitigate basic spoofing

## Comparison with Alternatives

| Feature | Traditional DNS (RFC 1035) | DNS over HTTPS (DoH) | DNS over TLS (DoT) |
|---------|---------------------------|---------------------|---------------------|
| Transport | UDP/53 | HTTPS/443 | TLS/853 |
| Encryption | None | TLS | TLS |
| Privacy | Low | High | High |
| Implemented here | Yes | No | No |

## Possible Viva Questions

**Q1: Why does DNS primarily use UDP instead of TCP?**  
A: DNS queries and responses are typically small (<512 bytes) and can fit in a single datagram. UDP avoids the overhead of TCP's 3-way handshake, making resolution faster. TCP is used for zone transfers or truncated responses.

**Q2: How is a domain name encoded in DNS wire format?**  
A: Each label is prefixed with its length byte, followed by the ASCII characters. The sequence terminates with a zero byte. For example, `www.google.com` becomes: `[3]www[6]google[3]com[0]`. This is implemented in `DnsPacket.encodeDomainName()`.

**Q3: What are DNS compression pointers and why are they needed?**  
A: When the same domain name appears multiple times in a packet, compression pointers (2 bytes starting with `0xC0`) reference the first occurrence to save space. The code handles this in `decodeNameFull()` by checking `(len & 0xC0) == 0xC0` and following the 14-bit offset.

**Q4: What record types does this implementation support?**  
A: A (IPv4, type 1), NS (type 2), CNAME (type 5), MX (type 15), AAAA (IPv6, type 28). Defined as constants in `DnsPacket`.

**Q5: What flag is set in the query to request recursive resolution?**  
A: The RD (Recursion Desired) bit is set to 1 in byte 2 of the header: `baos.write(0x01)` in `buildQuery()`.

---

# 3. SMTP (Simple Mail Transfer Protocol)

## Concept

SMTP is a text-based, command–response protocol used for sending email between mail servers. Defined by **RFC 5321**, it operates over TCP (port 25) and follows a strict sequential exchange of commands and 3-digit reply codes.

## Architecture

```
Mail Client (MUA)
       |
       | SMTP (TCP:25)
       v
Sending MTA ──SMTP──> Receiving MTA ──local delivery──> Recipient Mailbox
  (client)              (server)
```

**Communication Model**: Client–Server, command–response over a persistent TCP connection  
**Transport**: TCP (port 25; configurable via `Config.getSmtpPort()`)

## Message Format

### SMTP Commands (RFC 5321 §4.1)

| Command | Format | Purpose |
|---------|--------|---------|
| HELO | `HELO domain` | Identify client to server |
| EHLO | `EHLO domain` | Extended HELO (ESMTP) |
| MAIL FROM | `MAIL FROM:<sender@example.com>` | Specify envelope sender |
| RCPT TO | `RCPT TO:<recipient@example.com>` | Specify envelope recipient |
| DATA | `DATA` | Begin message body input |
| QUIT | `QUIT` | Close session |

### SMTP Reply Codes (RFC 5321 §4.2)

| Code | Meaning | Used in Code |
|------|---------|-------------|
| 220 | Service ready | `REPLY_READY` — expected on connect |
| 221 | Closing connection | `REPLY_CLOSING` — after QUIT |
| 250 | OK / action completed | `REPLY_OK` — after HELO, MAIL FROM, RCPT TO |
| 354 | Start mail input | `REPLY_START_INPUT` — after DATA |
| 4xx | Transient error | Not explicitly handled |
| 5xx | Permanent error | Throws `RuntimeException` |

### Email Message Structure (RFC 5322)

```
From: sender@example.com\r\n
To: recipient@example.com\r\n
Subject: Test Email\r\n
Date: 2026-02-24T12:00:00+05:30\r\n
MIME-Version: 1.0\r\n
Content-Type: text/plain; charset=UTF-8\r\n
\r\n
This is the body of the email.
\r\n.\r\n                               ← DATA terminator
```

## Workflow

### Full SMTP Session (`SmtpClient.sendEmail()`)

```
[Connect]  TCP handshake to server:25
    ← 220 service ready          (greeting)
[HELO]     → HELO client.local
    ← 250 OK
[MAIL FROM] → MAIL FROM:<sender@example.com>
    ← 250 OK
[RCPT TO]  → RCPT TO:<recipient@example.com>
    ← 250 OK
[DATA]     → DATA
    ← 354 Start mail input
[Body]     → From: ... To: ... Subject: ... body... \r\n.\r\n
    ← 250 OK (message accepted)
[QUIT]     → QUIT
    ← 221 Bye
[Close]    TCP FIN
```

### Multi-Line Reply Handling

SMTP servers may send multi-line replies where intermediate lines use a hyphen after the code (e.g., `250-Extended`). The final line uses a space (e.g., `250 OK`). This is handled in `SmtpClient.readReply()`:

```java
do {
    line = client.receiveLine();
    fullReply.append(line);
} while (SmtpCommand.isMultiLine(line));  // checks charAt(3) == '-'
```

## Code Mapping

| Concept | Source File | Key Method |
|---------|-----------|------------|
| Command building | `SmtpCommand.java` | `helo()`, `mailFrom()`, `rcptTo()`, `data()`, `quit()` |
| Reply parsing | `SmtpCommand.java` | `parseReplyCode()`, `parseReplyMessage()`, `isMultiLine()` |
| Success validation | `SmtpCommand.java` | `isSuccess()` (code >= 200 && code < 400) |
| Client session | `SmtpClient.java` | `connect()`, `helo()`, `mailFrom()`, `rcptTo()`, `sendData()`, `quit()` |
| Full email send | `SmtpClient.java` | `sendEmail()` — chains all steps |
| TCP transport | `SocketClient.java` | `sendLine()`, `sendRaw()`, `receiveLine()` |

### Design Patterns

- **Command Pattern**: `SmtpCommand` provides static factory methods for each command string
- **Template Method**: `sendEmail()` orchestrates the fixed sequence of protocol steps
- **Validation**: `sendCommand()` sends a command and validates the expected reply code, throwing on mismatch

## Advantages

- Simple text-based protocol — easy to debug via telnet
- Universally supported — every mail server speaks SMTP
- Reliable delivery — uses TCP with acknowledgment at each step

## Limitations

- **No encryption** — STARTTLS not implemented; messages sent in plaintext
- **No authentication** — AUTH extension not implemented
- **Single recipient only** — code calls `rcptTo()` once (could loop for multiple)
- **No attachment support** — body is plain text only, no multipart MIME
- **No error recovery** — RSET command defined in `SmtpCommand` but not invoked

## Security Considerations

- Plaintext communication — vulnerable to eavesdropping and tampering
- No sender verification — susceptible to email spoofing
- Requires an SMTP server that allows relay (many servers reject open relay)

## Possible Viva Questions

**Q1: What is the SMTP command sequence required to send an email?**  
A: `HELO → MAIL FROM → RCPT TO → DATA → message body → \r\n.\r\n → QUIT`. This is implemented in `SmtpClient.sendEmail()`.

**Q2: What does reply code 354 mean?**  
A: It means "Start mail input; end with `<CRLF>.<CRLF>`". The server is ready to accept the email body. Defined as `SmtpCommand.REPLY_START_INPUT`.

**Q3: How are multi-line SMTP replies handled?**  
A: Lines ending with a hyphen after the 3-digit code (e.g., `250-Hello`) indicate continuation. The last line uses a space (e.g., `250 OK`). `SmtpCommand.isMultiLine()` checks `reply.charAt(3) == '-'`.

**Q4: How is the email body terminated?**  
A: By sending `\r\n.\r\n` — a CRLF, a single dot, and another CRLF. This is returned by `SmtpCommand.dataTerminator()`.

**Q5: What happens if the server returns an unexpected reply code?**  
A: `SmtpClient.sendCommand()` compares the actual code against the expected code and throws a `RuntimeException` with the full error message if they differ.

---

# 4. FTP (File Transfer Protocol)

## Concept

FTP is a dual-connection protocol for file transfer between a client and server. Defined by **RFC 959**, it uses a **control connection** (TCP port 21) for commands and a separate **data connection** for file transfers and directory listings. This project implements passive (PASV) mode.

## Architecture

```
FTP Client                          FTP Server
    |                                    |
    |──── Control Connection (TCP:21) ──→|  Commands: USER, PASS, PASV, LIST, QUIT
    |                                    |  Replies: 220, 331, 230, 227, 150, 226
    |                                    |
    |──── Data Connection (TCP:P) ──────→|  File listings, file content
    |     (PORT from PASV response)      |
```

**Communication Model**: Client–Server with separate control and data channels  
**Transport**: TCP (port 21 for control; ephemeral port for data in PASV mode)

## Message Format

### FTP Commands (RFC 959 §4)

| Command | Format | Expected Reply |
|---------|--------|---------------|
| USER | `USER username` | 331 (password required) or 230 (logged in) |
| PASS | `PASS password` | 230 (login successful) |
| PASV | `PASV` | 227 with (h1,h2,h3,h4,p1,p2) |
| LIST | `LIST` | 150 (opening data connection) then 226 (transfer complete) |
| QUIT | `QUIT` | 221 (goodbye) |

### FTP Reply Code Categories (RFC 959 §4.2)

| Range | Meaning | Example |
|-------|---------|---------|
| 1xx | Positive Preliminary | 150 — Opening data connection |
| 2xx | Positive Completion | 220 Welcome, 226 Transfer complete, 230 Login OK |
| 3xx | Positive Intermediate | 331 — Password required |
| 4xx | Transient Negative | 425 — Cannot open data connection |
| 5xx | Permanent Negative | 550 — File not found |

### PASV Response Parsing

The 227 response contains the data connection address in format `(h1,h2,h3,h4,p1,p2)`:
- Host: `h1.h2.h3.h4`
- Port: `p1 × 256 + p2`

Implemented in `FtpClient.enterPassiveMode()`:
```java
String[] parts = msg.substring(start + 1, end).split(",");
String host = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
int dataPort = Integer.parseInt(parts[4].trim()) * 256 + Integer.parseInt(parts[5].trim());
```

## Workflow

### Complete FTP LIST Session (`FtpClient.listAndPrint()`)

```
[Connect]  TCP handshake to server:21
    ← 220 Welcome to FTP server
[USER]     → USER username
    ← 331 Password required
[PASS]     → PASS password
    ← 230 Login successful
[PASV]     → PASV
    ← 227 Entering Passive Mode (127,0,0,1,39,5)
           → parse: host=127.0.0.1, port=39×256+5=9989
[Data]     Open TCP to 127.0.0.1:9989
[LIST]     → LIST (on control connection)
    ← 150 Opening data connection
[Read]     Read listing data from data connection
           Close data connection
    ← 226 Transfer complete
[QUIT]     → QUIT
    ← 221 Goodbye
[Close]    Close control connection
```

## Code Mapping

| Concept | Source File | Key Method |
|---------|-----------|------------|
| Response parsing | `FtpResponse.java` | Constructor, `getCode()`, `getMessage()`, `isSuccess()`, `isComplete()`, `isMultiLine()` |
| Client operations | `FtpClient.java` | `connect()`, `login()`, `enterPassiveMode()`, `list()`, `quit()` |
| Full LIST session | `FtpClient.java` | `listAndPrint()` |
| Multi-line handling | `FtpClient.java` | `readResponse()` — loops while `charAt(3) == '-'` |
| TCP transport | `SocketClient.java` | `connect()`, `sendLine()`, `receiveLine()` |

### Design Patterns

- **Dual-Channel Architecture**: Separate `SocketClient` for control and raw `Socket` for data
- **State Machine**: Connection → Authentication → Passive Mode → Transfer → Disconnect
- **Response Object**: `FtpResponse` encapsulates parsing and classification of reply codes

## Advantages

- Separate data channel — file transfers don't block commands
- Supports large file transfers via streaming
- Passive mode works through NAT/firewalls (server opens data port)

## Limitations

- **No file upload/download** — only LIST implemented (STOR/RETR not present)
- **No active mode** — only PASV mode supported
- **Plaintext credentials** — USER/PASS sent unencrypted
- **No FTPS/SFTP** — no TLS or SSH-based secure transfer
- **Single data connection** — no parallel transfer support

## Possible Viva Questions

**Q1: Why does FTP use two separate connections?**  
A: The control connection (port 21) carries commands/replies and stays open for the session. The data connection (ephemeral port) carries file content or listings. This allows commands to be sent during a transfer and prevents data from being mixed with control messages.

**Q2: What is the difference between active and passive FTP modes?**  
A: In active mode, the server connects to the client's specified port (PORT command). In passive mode (PASV), the server opens a port and the client connects to it. This project uses PASV mode because it works better with firewalls/NAT.

**Q3: How is the data port calculated from the PASV response?**  
A: The response contains `(h1,h2,h3,h4,p1,p2)`. The port is `p1 × 256 + p2`. Implemented in `FtpClient.enterPassiveMode()`.

**Q4: What reply code indicates that the server is ready for a data transfer?**  
A: Code 150 ("File status okay; about to open data connection"). After the transfer, the server sends 226 ("Transfer complete").

---

# 5. DHCP (Dynamic Host Configuration Protocol)

## Concept

DHCP is a network management protocol for automatically assigning IP addresses and other configuration parameters to devices on a network. Defined by **RFC 2131** (with options in **RFC 2132**), it operates over UDP using a four-step exchange known as **DORA** (Discover → Offer → Request → Acknowledge). This project simulates the DORA sequence on localhost using non-standard ports.

## Architecture

```
DHCP Client                          DHCP Server
(needs IP)                           (has IP pool)
    |                                    |
    |── DISCOVER (broadcast) ──────────→|  "I need an IP address"
    |                                    |
    |←── OFFER ─────────────────────────|  "Here's 192.168.1.100"
    |                                    |
    |── REQUEST ───────────────────────→|  "I accept 192.168.1.100"
    |                                    |
    |←── ACK ───────────────────────────|  "192.168.1.100 is yours for 24h"
```

**Communication Model**: Client–Server via UDP broadcast (simulated on localhost)  
**Transport**: UDP (real: ports 67/68; simulated: 6700/6800 via `Config`)

## Packet Format (RFC 2131 §2)

```
 0               1               2               3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     op (1)    |   htype (1)   |   hlen (1)    |   hops (1)    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                            xid (4)                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|           secs (2)            |           flags (2)           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                          ciaddr (4)                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                          yiaddr (4)                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                          siaddr (4)                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                          giaddr (4)                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                          chaddr (16)                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                          sname (64)                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                          file (128)                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                    magic cookie (4): 99.130.83.99             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       options (variable)                      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### Key Fields

| Field | Size | Description | Code Reference |
|-------|------|-------------|---------------|
| op | 1 byte | 1=Request (client→server), 2=Reply | `OP_REQUEST`, `OP_REPLY` |
| htype | 1 byte | Hardware type (1=Ethernet) | `HTYPE_ETHERNET` |
| hlen | 1 byte | Hardware address length (6 for MAC) | `HLEN_ETHERNET` |
| xid | 4 bytes | Transaction ID (random) | `new Random().nextInt()` |
| flags | 2 bytes | 0x8000 = broadcast | Set in `createDiscover()` |
| ciaddr | 4 bytes | Client IP (if known) | `ciaddr` field |
| yiaddr | 4 bytes | "Your" IP (assigned by server) | `yiaddr` field |
| siaddr | 4 bytes | Server IP | `siaddr` field |
| chaddr | 16 bytes | Client hardware (MAC) address | `chaddr` field |
| Magic Cookie | 4 bytes | `99.130.83.99` (DHCP identifier) | `MAGIC_COOKIE` |

### DHCP Options (RFC 2132)

| Option Code | Name | Length | Constant |
|-------------|------|--------|----------|
| 1 | Subnet Mask | 4 | `OPT_SUBNET_MASK` |
| 3 | Router/Gateway | 4 | `OPT_ROUTER` |
| 6 | DNS Server | 4 | `OPT_DNS_SERVER` |
| 50 | Requested IP | 4 | `OPT_REQUESTED_IP` |
| 51 | Lease Time | 4 | `OPT_LEASE_TIME` |
| 53 | Message Type | 1 | `OPT_MSG_TYPE` |
| 54 | Server Identifier | 4 | `OPT_SERVER_ID` |
| 255 | End | 0 | `OPT_END` |

### DHCP Message Types (Option 53)

| Value | Type | Direction | Constant |
|-------|------|-----------|----------|
| 1 | DISCOVER | Client → Server | `DHCP_DISCOVER` |
| 2 | OFFER | Server → Client | `DHCP_OFFER` |
| 3 | REQUEST | Client → Server | `DHCP_REQUEST` |
| 4 | DECLINE | Client → Server | `DHCP_DECLINE` |
| 5 | ACK | Server → Client | `DHCP_ACK` |
| 6 | NAK | Server → Client | `DHCP_NAK` |
| 7 | RELEASE | Client → Server | `DHCP_RELEASE` |

## Workflow

### DORA Sequence (`DhcpSimClient.runDora()`)

```
[Client]  createDiscover(macAddress) → toBytes() → send to 127.0.0.1:6700
    → DISCOVER: op=1, xid=random, flags=0x8000, chaddr=AA:BB:CC:DD:EE:FF

[Server]  receive → fromBytes() → type=DISCOVER
    → createOffer(discover, "192.168.1.100", ...) → toBytes() → reply
    ← OFFER: op=2, yiaddr=192.168.1.100, siaddr=192.168.1.1,
             subnet=255.255.255.0, gateway=192.168.1.1, dns=8.8.8.8, lease=86400s

[Client]  receive → fromBytes() → parse offered IP, server IP
    → createRequest(discover, offeredIp, serverIp) → toBytes() → send
    → REQUEST: op=1, requestedIp=192.168.1.100, serverIdentifier=192.168.1.1

[Server]  receive → fromBytes() → type=REQUEST
    → createAck(request, "192.168.1.100", ...) → toBytes() → reply
    ← ACK: op=2, yiaddr=192.168.1.100, confirmed configuration

[Client]  "IP 192.168.1.100 assigned! DORA complete."
```

### Server Loop (`DhcpSimServer.start()`)

```
Bind UdpClient to port 6700
Loop (max 2 exchanges):
  receive(576) → DhcpPacket.fromBytes()
  if DISCOVER → createOffer() → send reply
  if REQUEST  → createAck()   → send reply
  else → ignore
Finally: stop()
```

## Code Mapping

| Concept | Source File | Key Method |
|---------|-----------|------------|
| Packet construction | `DhcpPacket.java` | `createDiscover()`, `createOffer()`, `createRequest()`, `createAck()` |
| Serialization | `DhcpPacket.java` | `toBytes()` — 576 bytes minimum, options appended |
| Deserialization | `DhcpPacket.java` | `fromBytes()` — parses header, address fields, options |
| Option encoding | `DhcpPacket.java` | `toBytes()` — conditionally writes options 1,3,6,50,51,53,54 |
| Option decoding | `DhcpPacket.java` | `fromBytes()` — switch on option code |
| Server | `DhcpSimServer.java` | `start()`, `startInBackground()`, `stop()` |
| Client | `DhcpSimClient.java` | `runDora()`, `formatMac()` |
| UDP transport | `UdpClient.java` | `send()`, `receive()` |
| Byte utilities | `ByteUtils.java` | `ipToBytes()`, `bytesToIp()`, `readUint32()`, `writeUint32()` |

### Design Patterns

- **Factory Methods**: `createDiscover()`, `createOffer()`, `createRequest()`, `createAck()` — static factories for each message type
- **Simulation Pattern**: `DhcpSimServer` and `DhcpSimClient` operate on localhost with non-standard ports, avoiding privileged port restrictions
- **State Machine**: Server processes exactly 2 exchanges (DISCOVER→OFFER, REQUEST→ACK) then stops

## Advantages

- Automatic IP assignment eliminates manual configuration
- Centralized management of network parameters (subnet, gateway, DNS)
- Lease mechanism allows IP address reuse

## Limitations

- **Simulated only** — runs on localhost, not on real network interfaces
- **No IP pool management** — always offers the same hardcoded IP (`192.168.1.100`)
- **No lease tracking** — no database of assigned leases
- **Fixed to 2 exchanges** — server stops after one DORA cycle
- **No DHCPNAK handling** — client does not handle negative acknowledgments
- **No broadcast** — uses direct UDP to localhost instead of real broadcast

## Security Considerations

- DHCP has no built-in authentication — any device on the network can request an IP
- Rogue DHCP servers can redirect traffic (DHCP spoofing)
- No encryption of configuration data

## Comparison with Static IP Configuration

| Feature | DHCP | Static IP |
|---------|------|-----------|
| Configuration | Automatic | Manual |
| Scalability | High | Low |
| IP conflicts | Managed by server | Possible |
| Implementation here | Full DORA simulation | Not applicable |

## Possible Viva Questions

**Q1: What does DORA stand for?**  
A: Discover, Offer, Request, Acknowledge — the four-step exchange for obtaining an IP address. Implemented across `DhcpSimClient.runDora()` and `DhcpSimServer.start()`.

**Q2: Why does (real) DHCP use UDP instead of TCP?**  
A: The client has no IP address yet, so it cannot complete a TCP handshake. DHCP uses UDP broadcast from 0.0.0.0:68 to 255.255.255.255:67.

**Q3: What is the DHCP magic cookie and why is it needed?**  
A: `99.130.83.99` (hex: `63.82.53.63`) placed at byte offset 236. It marks the start of the options section and distinguishes DHCP from the older BOOTP protocol. Defined as `DhcpPacket.MAGIC_COOKIE`.

**Q4: What information does a DHCP OFFER contain?**  
A: The offered IP (`yiaddr`), server IP (`siaddr`), subnet mask (option 1), gateway (option 3), DNS server (option 6), and lease time (option 51). All parsed in `DhcpSimClient.runDora()`.

**Q5: Why does the simulation use ports 6700/6800?**  
A: Real DHCP uses privileged ports 67 (server) and 68 (client) which require root/administrator access. Non-standard high ports allow the demo to run without elevated privileges.

---

# C. Architecture Deep-Dive

## Layered Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Main.java                        │  Entry Point + CLI Parsing
│               (menu-driven demos)                   │
├──────────┬───────┬────────┬─────────┬───────────────┤
│   HTTP   │  DNS  │  SMTP  │   FTP   │     DHCP      │  Protocol Layer
│ Client/  │Client │Client/ │ Client/ │  SimClient/   │
│ Server   │       │Command │Response │   SimServer   │
├──────────┴───────┴────────┴─────────┴───────────────┤
│              Core Infrastructure Layer              │
│  ProtocolHandler │ SocketClient │ UdpClient │TcpServer│
├─────────────────────────────────────────────────────┤
│                 Utilities Layer                      │
│         Config │ Logger │ ByteUtils                  │
├─────────────────────────────────────────────────────┤
│              Tests Layer                             │
│   HttpTest │ DnsTest │ SmtpTest │ FtpTest │ DhcpTest │
└─────────────────────────────────────────────────────┘
```

## Design Patterns Summary

| Pattern | Where Used | Purpose |
|---------|-----------|---------|
| Strategy | `ProtocolHandler` + `TcpServer` | Decouple server infrastructure from protocol logic; `HttpServer`/`MockSmtpHandler`/`MockFtpHandler` all implement the same interface |
| Factory Method | `DhcpPacket.createDiscover/Offer/Request/Ack()` | Construct protocol-specific packet variants |
| Builder | `HttpRequest`, `HttpResponse` | Programmatic message construction via `addHeader()`, `setBody()` |
| Template Method | `SmtpClient.sendEmail()`, `FtpClient.listAndPrint()` | Fixed sequence of protocol operations |
| AutoCloseable | `SocketClient`, `UdpClient` | Guaranteed resource cleanup via try-with-resources |
| Command | `SmtpCommand` static methods | Encapsulate command string formation |

## Transport Layer Mapping

| Standard Library Class | Wrapper in Project | Used By |
|----------------------|-------------------|---------|
| `java.net.Socket` | `SocketClient` | HTTP, SMTP, FTP (control) |
| `java.net.ServerSocket` | `TcpServer` | HTTP server, test mock servers |
| `java.net.DatagramSocket` | `UdpClient` | DNS, DHCP |
| `java.net.Socket` (raw) | Direct in `FtpClient.list()` | FTP data connection |

## Thread Model

- `TcpServer` creates one daemon thread per accepted connection
- `HttpServer`, `DhcpSimServer` support `startInBackground()` for non-blocking startup
- DHCP server uses `volatile boolean running` for graceful shutdown
- No thread pool — thread-per-connection model appropriate for demonstration

---

# D. File-by-File Code Explanation

## Core Layer (4 files)

| File | Lines | Purpose |
|------|-------|---------|
| `ProtocolHandler.java` | 22 | Interface defining `handle(Socket)` — Strategy pattern entry point |
| `SocketClient.java` | 185 | TCP wrapper: connect, send (line/raw), receive (line/all), timeouts, AutoCloseable |
| `UdpClient.java` | 112 | UDP wrapper: send, receive, port binding, timeouts, AutoCloseable |
| `TcpServer.java` | 141 | Multi-threaded server: accept loop, thread-per-connection, background start, graceful stop |

## Protocol Layer (13 files)

| File | Lines | Purpose |
|------|-------|---------|
| `HttpRequest.java` | 149 | HTTP request model: `parse()` from raw text, `serialize()` to wire format, headers via `LinkedHashMap` |
| `HttpResponse.java` | 158 | HTTP response model: `parse()`/`serialize()`, auto `Content-Length` injection |
| `HttpClient.java` | 92 | HTTP GET client: builds `HttpRequest`, sends via `SocketClient`, parses `HttpResponse` |
| `HttpServer.java` | 165 | HTTP server: implements `ProtocolHandler`, routes `/`, `/status`, handles 404/405 |
| `DnsPacket.java` | 335 | DNS packet: binary query builder, response decoder, compression pointer handling, record types |
| `DnsClient.java` | 108 | DNS client: `resolve()` sends query via `UdpClient`, decodes response |
| `SmtpCommand.java` | 137 | SMTP command builder: `helo()`, `mailFrom()`, `rcptTo()`, reply parsing, multi-line detection |
| `SmtpClient.java` | 218 | SMTP client: full session (connect→HELO→MAIL FROM→RCPT TO→DATA→QUIT), multi-line reply handling |
| `FtpResponse.java` | 106 | FTP response parser: code extraction, success/complete/multi-line classification |
| `FtpClient.java` | 233 | FTP client: connect, login, PASV mode, LIST via data connection, quit |
| `DhcpPacket.java` | 413 | DHCP packet: factory methods for DORA messages, `toBytes()`/`fromBytes()`, option encoding/decoding |
| `DhcpSimServer.java` | 132 | DHCP simulation server: receives DISCOVER/REQUEST, replies OFFER/ACK, 2-exchange limit |
| `DhcpSimClient.java` | 105 | DHCP simulation client: full DORA sequence on localhost |

## Utility Layer (3 files)

| File | Lines | Purpose |
|------|-------|---------|
| `Config.java` | 139 | Global configuration: ports, timeouts, DNS server, verbose mode. Thread-safe `volatile` for verbose flag |
| `Logger.java` | 88 | Structured logging: timestamp + level + thread name. DEBUG gated by `Config.isVerbose()`. ERROR→stderr |
| `ByteUtils.java` | 164 | Binary helpers: `readUint16/32()`, `writeUint16/32()`, `bytesToIp()`, `ipToBytes()`, `hexDump()` |

## Test Layer (5 files)

| File | Lines | Purpose |
|------|-------|---------|
| `HttpTest.java` | 174 | 7 tests: request/response parse/serialize, auto Content-Length, invalid input, client-server integration |
| `DnsTest.java` | 145 | 5 tests: query construction, packet size, domain encoding, type constants, live resolution |
| `SmtpTest.java` | 202 | 10 tests: command formatting (5), reply parsing (3), success validation, mock server session |
| `FtpTest.java` | 200 | 6 tests: response parsing, codes, multi-line, success/complete, mock FTP session with PASV |
| `DhcpTest.java` | 193 | 8 tests: DORA packet creation (4), serialize/deserialize round-trip, message type strings, magic cookie, simulated DORA |

---

# E. Testing Strategy

## Test Architecture

All tests use a custom assertion framework (no JUnit dependency):
- `assertEqual(name, expected, actual)` — exact string match
- `assertThat(name, condition)` — boolean condition
- `assertContains(name, haystack, needle)` — substring check
- `pass(msg)` / `fail(msg)` — increment counters + console output

Tests are standalone `main()` programs: `java tests.HttpTest`, `java tests.DnsTest`, etc.

## Test Categories

### Unit Tests
- **HTTP**: Request parsing, response parsing, serialization fidelity, auto Content-Length, invalid input rejection
- **DNS**: Query packet structure (header fields, flags, QDCOUNT=1, ANCOUNT=0), packet size calculation (29 bytes for `example.com`), domain label encoding verification at byte level, type constant values
- **SMTP**: Command string formatting (`HELO client.example.com`, `MAIL FROM:<user@example.com>`), reply code parsing (220, 250, 354, null, short string), multi-line detection, success range validation (200-399)
- **FTP**: Response code extraction (220, 230, 331, 550, null), multi-line detection, success classification (1xx-3xx), completion classification (2xx)
- **DHCP**: DORA packet field verification (op, messageType, MAC, yiaddr), serialize→deserialize round-trip (xid, op, type, MAC preserved), message type string mapping, magic cookie byte values

### Integration Tests
- **HTTP**: Starts `HttpServer` on port 9090, sends GET `/` (expects 200 + "Java Application Layer"), sends GET `/nonexistent` (expects 404), verifies server shutdown
- **DNS**: Sends real query to `8.8.8.8:53` for `example.com`, verifies answer contains A record with IP. Gracefully skips if no internet.
- **SMTP**: Starts `TcpServer` with `MockSmtpHandler` on port 9025. Mock handler simulates full SMTP: greeting→HELO→MAIL FROM→RCPT TO→DATA→body→QUIT. `SmtpClient` performs the complete session.
- **FTP**: Starts `TcpServer` with `MockFtpHandler` on port 9021. Mock handles USER/PASS/PASV/LIST/QUIT with ephemeral data socket. Client verifies welcome (220), login (230), quit (221).
- **DHCP**: Starts `DhcpSimServer` on port 9067, runs `DhcpSimClient.runDora()`, verifies full DORA exchange completes without exceptions.

## Test Summary

| Protocol | Unit Tests | Integration Tests | Total |
|----------|-----------|------------------|-------|
| HTTP | 6 | 1 (client-server) | 7 |
| DNS | 4 | 1 (live resolution) | 5 |
| SMTP | 9 | 1 (mock server) | 10 |
| FTP | 5 | 1 (mock server) | 6 |
| DHCP | 7 | 1 (simulated DORA) | 8 |
| **Total** | **31** | **5** | **36** |

---

# F. Protocol Comparison Matrix

| Feature | HTTP | DNS | SMTP | FTP | DHCP |
|---------|------|-----|------|-----|------|
| **RFC** | 2616 | 1035 | 5321 | 959 | 2131/2132 |
| **Transport** | TCP | UDP | TCP | TCP (dual) | UDP |
| **Default Port** | 80 | 53 | 25 | 21 (control) | 67/68 |
| **Format** | Text | Binary | Text | Text | Binary |
| **Connection** | Short-lived | Stateless | Session | Session | Stateless |
| **Direction** | Bidirectional | Query-Response | Command-Response | Command-Response | Broadcast-Unicast |
| **Java Wrapper** | SocketClient | UdpClient | SocketClient | SocketClient | UdpClient |
| **Server Impl** | Yes (HttpServer) | No | No (mock only) | No (mock only) | Yes (DhcpSimServer) |
| **Lines of Code** | 564 | 443 | 355 | 339 | 650 |
| **Test Count** | 7 | 5 | 10 | 6 | 8 |

---

# G. FAQ — Cross-Cutting Questions

**Q1: What design pattern is central to the project's server architecture?**  
A: The **Strategy Pattern**. `TcpServer` accepts a `ProtocolHandler` instance and delegates `handle(Socket)`. `HttpServer` implements `ProtocolHandler` for HTTP; test mock servers implement it for SMTP and FTP. This decouples the server's accept/thread logic from protocol-specific processing.

**Q2: Why are no third-party libraries used?**  
A: To demonstrate low-level understanding of protocol mechanics. Using libraries like Apache HttpClient or Netty would abstract away the very details this project aims to teach — raw byte manipulation, RFC-compliant message formatting, and socket I/O.

**Q3: How does this project handle binary vs. text protocols?**  
A: HTTP, SMTP, and FTP are text-based — they use `SocketClient` with `sendLine()`/`receiveLine()` (BufferedReader/PrintWriter). DNS and DHCP are binary — they use `UdpClient` with raw `byte[]` arrays, and `ByteUtils` for reading/writing multi-byte integers in big-endian (network byte order).

**Q4: What is network byte order and where is it used?**  
A: Big-endian, where the most significant byte comes first. Used in DNS packet headers (`ByteUtils.readUint16/32()`) and DHCP packet fields (`ByteUtils.writeUint16/32()`). For example, the integer 300 (0x012C) is stored as bytes `0x01, 0x2C`.

**Q5: How does the project ensure resources are properly closed?**  
A: `SocketClient` and `UdpClient` implement `AutoCloseable`, enabling try-with-resources blocks. For example, `DnsClient.resolve()` wraps `UdpClient` in `try (UdpClient udpClient = new UdpClient(...))`, guaranteeing the socket is closed even if an exception occurs.

**Q6: What is the difference between `sendLine()` and `sendRaw()`?**  
A: `sendLine()` appends `\r\n` (CRLF) to the data before sending — used for line-oriented protocols like SMTP/FTP. `sendRaw()` sends data as-is — used for HTTP request serialization and SMTP message bodies where CRLF management is handled by the caller.

**Q7: How does the project handle configuration?**  
A: `Config.java` holds static fields with default values (e.g., DNS=8.8.8.8, HTTP port=8080, timeouts=5000ms). The `verbose` field is `volatile` for thread-safe reads from logging threads. `Main.java` parses CLI arguments and calls the corresponding `Config.set*()` methods.

**Q8: What would need to change to support HTTPS?**  
A: Replace `Socket` with `SSLSocket` (from `javax.net.ssl.SSLSocketFactory`), configure TLS certificates, and update `SocketClient` to accept an SSL context. The HTTP request/response logic would remain the same since TLS operates at the transport layer.

**Q9: What is the significance of 576 bytes in DHCP?**  
A: RFC 2131 specifies a minimum DHCP packet size of 300 bytes, but 576 bytes is the minimum IP datagram size that all hosts must support (RFC 791). The `DhcpPacket.toBytes()` method allocates a 576-byte array to ensure compatibility.

**Q10: How would you add a new protocol (e.g., NTP)?**  
A: (1) Create `protocols/ntp/` package with packet model and client classes. (2) Since NTP uses UDP, reuse `UdpClient`. (3) Add an NTP menu option in `Main.java`. (4) Add NTP port configuration to `Config.java`. (5) Create `tests/NtpTest.java`. The layered architecture makes this a modular addition with no changes to existing protocols.

---

# H. Conclusion

This project demonstrates a complete, from-scratch implementation of five Application Layer protocols in Java. By operating directly on TCP sockets and UDP datagrams — without any third-party libraries — it provides deep insight into how protocols like HTTP, DNS, SMTP, FTP, and DHCP actually work at the wire level.

**Key achievements:**
- **2,351+ lines** of protocol implementation across 13 source files
- **36 automated tests** covering both unit and integration scenarios
- **5 RFC-conformant** protocol implementations (RFC 2616, 1035, 5321, 959, 2131)
- **Zero dependencies** — pure Java using only standard library classes
- **Production patterns** — Strategy, Factory, Builder, AutoCloseable, Template Method

The layered architecture (Core → Protocol → Utility → Test) ensures that adding a new protocol requires no modification to existing code, following the Open/Closed Principle. The comprehensive test suite validates not just message formatting but end-to-end communication through actual network I/O with mock servers and simulated exchanges.

---

*End of Academic Theory Document*
