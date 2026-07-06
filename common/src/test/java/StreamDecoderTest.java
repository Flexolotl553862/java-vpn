import com.stevesad.common.consumer.ByteBufStreamDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.function.Consumer;

import static org.mockito.Mockito.times;

public class StreamDecoderTest {

    private final Consumer<ByteBuf> consumer = Mockito.mock();
    private final ByteBufStreamDecoder decoder = new ByteBufStreamDecoder(consumer);

    byte[] packet1 = new byte[] {
            0x45, 0x00, 0x00, 0x1C,
            0x12, 0x34, 0x40, 0x00,
            0x40, 0x01, 0x00, 0x00,
            (byte) 192, (byte) 168, 1, 10,
            (byte) 192, (byte) 168, 1, 20,

            0x08, 0x00, 0x00, 0x00,
            0x00, 0x01, 0x00, 0x01
    };

    byte[] packet2 = new byte[] {
            0x45, 0x00, 0x00, 0x1C,
            0x56, 0x78, 0x40, 0x00,
            0x40, 0x01, 0x00, 0x00,
            (byte) 10, 0, 0, 1,
            (byte) 10, 0, 0, 2,

            0x08, 0x00, 0x00, 0x00,
            0x00, 0x02, 0x00, 0x02
    };

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 7, 15, 18, 22, 37, 56})
    public void testHandle(int len) {
        byte[] frame = new byte[56];

        System.arraycopy(packet1, 0, frame, 0, 28);
        System.arraycopy(packet2, 0, frame, 28, 28);

        for (int i = 0; i < 56; i += len) {
            int curLen = Math.min(len, 56 - i);
            byte[] part = new byte[curLen];
            System.arraycopy(frame, i, part, 0, curLen);
            decoder.handle(Unpooled.copiedBuffer(part));
        }

        verify();
    }

    private void verify() {
        ArgumentCaptor<ByteBuf> captor =
                ArgumentCaptor.forClass(ByteBuf.class);

        Mockito.verify(consumer, times(2)).accept(captor.capture());

        List<ByteBuf> captured = captor.getAllValues();

        byte[] first = new byte[captured.get(0).readableBytes()];
        captured.get(0).getBytes(0, first);

        byte[] second = new byte[captured.get(1).readableBytes()];
        captured.get(1).getBytes(0, second);

        Assertions.assertArrayEquals(packet1, first);
        Assertions.assertArrayEquals(packet2, second);
    }
}
