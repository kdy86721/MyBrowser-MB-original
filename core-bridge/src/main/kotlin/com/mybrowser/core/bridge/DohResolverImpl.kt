package com.mybrowser.core.bridge

import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Pure Kotlin RFC 1035 DNS wire-format builder & parser for DNS-over-HTTPS (RFC 8484).
 * Operates strictly on `ByteArray` + flat JSON across the FFI boundary.
 */
// [RUST-MIGRATION-TARGET]
// // Replaceable with `hickory-proto` (`hickory_proto::op::Message`) in Rust.
// pub fn build_dns_query_packet(hostname: String, record_type: i32) -> Vec<u8>
// pub fn parse_dns_response_packet(wire_bytes: Vec<u8>) -> String
class DohResolverImpl : DohResolverContract {

  private data class DohProviderSpec(
    val id: String,
    val label: String,
    val endpointUrl: String,
    val jurisdiction: String,
  )

  private val providers =
    listOf(
      DohProviderSpec(
        id = "cloudflare",
        label = "Cloudflare 1.1.1.1",
        endpointUrl = "https://cloudflare-dns.com/dns-query",
        jurisdiction = "Global",
      ),
      DohProviderSpec(
        id = "quad9",
        label = "Quad9",
        endpointUrl = "https://dns.quad9.net/dns-query",
        jurisdiction = "Switzerland",
      ),
      DohProviderSpec(
        id = "adguard",
        label = "AdGuard DNS",
        endpointUrl = "https://dns.adguard-dns.com/dns-query",
        jurisdiction = "Cyprus",
      ),
      DohProviderSpec(
        id = "mullvad",
        label = "Mullvad Adblock",
        endpointUrl = "https://adblock.dns.mullvad.net/dns-query",
        jurisdiction = "Sweden",
      ),
      DohProviderSpec(
        id = "custom",
        label = "사용자 지정",
        endpointUrl = "https://dns.quad9.net/dns-query",
        jurisdiction = "Custom",
      ),
    )

  override fun buildDnsQueryPacket(hostname: String, recordType: Int): ByteArray {
    val cleanHost = hostname.trim().trimEnd('.').lowercase()
    val out = ByteArrayOutputStream(64)

    // RFC 1035 Header (12 bytes): ID=0x4D42 ("MB"), Flags=0x0100 (RD=1), QDCOUNT=1
    out.write(0x4D)
    out.write(0x42)
    out.write(0x01)
    out.write(0x00)
    out.write(0x00)
    out.write(0x01) // QDCOUNT = 1
    out.write(0x00)
    out.write(0x00) // ANCOUNT = 0
    out.write(0x00)
    out.write(0x00) // NSCOUNT = 0
    out.write(0x00)
    out.write(0x00) // ARCOUNT = 0

    if (cleanHost.isNotEmpty()) {
      for (label in cleanHost.split('.')) {
        val bytes = label.toByteArray(StandardCharsets.US_ASCII)
        val len = bytes.size.coerceAtMost(63)
        out.write(len)
        out.write(bytes, 0, len)
      }
    }
    out.write(0x00) // Root terminator

    val qType = if (recordType == TYPE_AAAA) TYPE_AAAA else TYPE_A
    out.write((qType ushr 8) and 0xFF)
    out.write(qType and 0xFF)
    out.write(0x00)
    out.write(0x01)

    return out.toByteArray()
  }

  override fun parseDnsResponsePacket(wireBytes: ByteArray): String {
    if (wireBytes.size < 12) {
      return """{"ok":false,"error":"Malformed DNS packet (<12 bytes)","records":[]}"""
    }
    return try {
      val buf = ByteBuffer.wrap(wireBytes)
      val id = buf.short.toInt() and 0xFFFF
      val flags = buf.short.toInt() and 0xFFFF
      val rcode = flags and 0x0F
      val qdCount = buf.short.toInt() and 0xFFFF
      val anCount = buf.short.toInt() and 0xFFFF
      buf.short
      buf.short

      repeat(qdCount) {
        skipDnsName(buf)
        if (buf.remaining() >= 4) {
          buf.short
          buf.short
        }
      }

      val records = mutableListOf<String>()
      repeat(anCount) {
        if (buf.remaining() < 12) return@repeat
        skipDnsName(buf)
        if (buf.remaining() < 10) return@repeat
        val rType = buf.short.toInt() and 0xFFFF
        buf.short
        val ttl = buf.int.toLong() and 0xFFFFFFFFL
        val rdLength = buf.short.toInt() and 0xFFFF
        if (buf.remaining() < rdLength) return@repeat
        val rdata = ByteArray(rdLength)
        buf.get(rdata)

        if ((rType == TYPE_A && rdLength == 4) || (rType == TYPE_AAAA && rdLength == 16)) {
          val addr = InetAddress.getByAddress(rdata).hostAddress ?: ""
          val typeLabel = if (rType == TYPE_A) "A" else "AAAA"
          records += """{"type":${JsonMini.quote(typeLabel)},"address":${JsonMini.quote(addr)},"ttl":$ttl}"""
        }
      }

      """{"ok":${rcode == 0},"txId":$id,"rcode":$rcode,"answerCount":$anCount,"records":[${records.joinToString(",")}]}"""
    } catch (e: Exception) {
      """{"ok":false,"error":${JsonMini.quote(e.message ?: "Parse error")},"records":[]}"""
    }
  }

  override fun getDohProvidersJson(): String {
    val items =
      providers.joinToString(",") { p ->
        """{"id":${JsonMini.quote(p.id)},"label":${JsonMini.quote(p.label)},"endpointUrl":${JsonMini.quote(p.endpointUrl)},"jurisdiction":${JsonMini.quote(p.jurisdiction)}}"""
      }
    return "[$items]"
  }

  private fun skipDnsName(buf: ByteBuffer) {
    while (buf.hasRemaining()) {
      val len = buf.get().toInt() and 0xFF
      if (len == 0) break
      if ((len and 0xC0) == 0xC0) {
        if (buf.hasRemaining()) buf.get()
        break
      }
      val skip = len.coerceAtMost(buf.remaining())
      buf.position(buf.position() + skip)
    }
  }

  companion object {
    const val TYPE_A = 1
    const val TYPE_AAAA = 28
  }
}
