import com.stevesad.common.utils.PacketUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class PacketUtilsTest {

    @Test
    public void testSrcAddressExtraction() throws UnknownHostException {
        InetAddress expectedAddress = Inet4Address.ofLiteral("192.168.1.10");
        InetAddress address = PacketUtils.extractIpV4SrcAddress(testIpV4Packet);
        Assertions.assertEquals(expectedAddress, address);
        Assertions.assertEquals(0, testIpV4Packet.readerIndex());
    }

    @Test
    public void testDstAddressExtraction() throws UnknownHostException {
        InetAddress expectedAddress = Inet4Address.ofLiteral("8.8.8.8");
        InetAddress address = PacketUtils.extractIpV4DstAddress(testIpV4Packet);
        Assertions.assertEquals(expectedAddress, address);
        Assertions.assertEquals(0, testIpV4Packet.readerIndex());
    }

    private final ByteBuf testIpV4Packet = Unpooled.wrappedBuffer(new byte[] {
        0x45,
        0x00,
        0x00,
        0x1c,
        0x12,
        0x34,
        0x40,
        0x00,
        0x40,
        0x01,
        (byte) 0xa5,
        0x4b,
        (byte) 0xc0,
        (byte) 0xa8,
        0x01,
        0x0a,
        0x08,
        0x08,
        0x08,
        0x08,
        0x08,
        0x00,
        (byte) 0xf7,
        (byte) 0xff,
        0x12,
        0x34,
        0x00,
        0x01
    });
}
